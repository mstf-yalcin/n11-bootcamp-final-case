package com.n11.bootcamp.stock_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.stock_service.dto.request.CreateStockRequest;
import com.n11.bootcamp.stock_service.dto.request.UpdateStockRequest;
import com.n11.bootcamp.stock_service.dto.response.ReservationResponse;
import com.n11.bootcamp.stock_service.dto.response.StockAvailabilityResponse;
import com.n11.bootcamp.stock_service.dto.response.StockResponse;
import com.n11.bootcamp.stock_service.entity.OutboxEvent;
import com.n11.bootcamp.stock_service.entity.ReservationStatus;
import com.n11.bootcamp.stock_service.entity.Stock;
import com.n11.bootcamp.stock_service.entity.StockReservation;
import com.n11.bootcamp.stock_service.entity.StockStatus;
import com.n11.bootcamp.common_lib.event.AggregateType;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.order.OrderCreatedPayload;
import com.n11.bootcamp.common_lib.event.order.OrderEventItem;
import com.n11.bootcamp.common_lib.event.stock.StockFailedPayload;
import com.n11.bootcamp.common_lib.event.stock.StockReservedPayload;
import com.n11.bootcamp.stock_service.exception.InvalidStockQuantityException;
import com.n11.bootcamp.stock_service.exception.StockAlreadyExistsException;
import com.n11.bootcamp.stock_service.exception.StockNotFoundException;
import com.n11.bootcamp.stock_service.mapper.StockMapper;
import com.n11.bootcamp.stock_service.repository.StockRepository;
import com.n11.bootcamp.stock_service.repository.OutboxEventRepository;
import com.n11.bootcamp.stock_service.repository.StockReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class StockService {

    private static final String AGGREGATE_TYPE = AggregateType.STOCK;

    private final StockRepository stockRepository;
    private final StockReservationRepository reservationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StockMapper stockMapper;
    private final ObjectMapper objectMapper;
    private final int lowStockThreshold;

    public StockService(StockRepository stockRepository,
                        StockReservationRepository reservationRepository,
                        OutboxEventRepository outboxEventRepository,
                        StockMapper stockMapper,
                        ObjectMapper objectMapper,
                        @Value("${app.stock.low-threshold:5}") int lowStockThreshold) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.stockMapper = stockMapper;
        this.objectMapper = objectMapper;
        this.lowStockThreshold = lowStockThreshold;
    }

    public List<StockResponse> getAll() {
        return stockRepository.findAllByIsActiveTrue()
                .stream()
                .map(stockMapper::toResponse)
                .toList();
    }

    public StockResponse getByProductId(UUID productId) {
        Stock stock = stockRepository.findByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new StockNotFoundException(productId));
        return stockMapper.toResponse(stock);
    }

    public List<StockAvailabilityResponse> getAvailability(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, Stock> stockMap = stockRepository.findAllByProductIdInAndIsActiveTrue(productIds)
                .stream()
                .collect(Collectors.toMap(Stock::getProductId, s -> s));

        return productIds.stream()
                .map(pid -> {
                    Stock stock = stockMap.get(pid);
                    int available = stock != null ? stock.getAvailable() : 0;
                    return new StockAvailabilityResponse(pid, available, classify(available));
                })
                .toList();
    }

    private StockStatus classify(int available) {
        if (available <= 0) return StockStatus.OUT_OF_STOCK;
        if (available <= lowStockThreshold) return StockStatus.LOW_STOCK;
        return StockStatus.IN_STOCK;
    }

    public List<ReservationResponse> getReservationsByOrderId(UUID orderId) {
        return reservationRepository.findAllByOrderId(orderId)
                .stream()
                .map(stockMapper::toReservationResponse)
                .toList();
    }

    @Transactional
    public StockResponse createStock(CreateStockRequest request) {
        log.info("Creating stock entry: productId={}, quantity={}", request.productId(), request.quantity());
        if (stockRepository.existsByProductIdAndIsActiveTrue(request.productId())) {
            throw new StockAlreadyExistsException(request.productId());
        }
        Stock stock = Stock.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .reserved(0)
                .build();
        Stock saved = stockRepository.save(stock);
        log.info("Stock created: id={}, productId={}", saved.getId(), saved.getProductId());
        return stockMapper.toResponse(saved);
    }

    @Transactional
    public StockResponse updateStock(UUID productId, UpdateStockRequest request) {
        log.info("Updating stock: productId={}, newQuantity={}", productId, request.quantity());
        Stock stock = stockRepository.findByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new StockNotFoundException(productId));
        if (request.quantity() < stock.getReserved()) {
            throw new InvalidStockQuantityException(productId, request.quantity(), stock.getReserved());
        }
        stock.setQuantity(request.quantity());
        Stock saved = stockRepository.save(stock);
        log.info("Stock updated: productId={}, quantity={}", productId, saved.getQuantity());
        return stockMapper.toResponse(saved);
    }

    @Transactional
    public void deleteStock(UUID productId) {
        log.info("Deleting stock (soft): productId={}", productId);
        Stock stock = stockRepository.findByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new StockNotFoundException(productId));
        stock.setActive(false);
        stockRepository.save(stock);
    }

    @Transactional
    public void reserveStock(OrderCreatedPayload order, String correlationId) {
        log.info("Reserving stock for orderId={}, correlationId={}", order.orderId(), correlationId);

        //TODO: change processed_events table
        if (reservationRepository.existsByOrderId(order.orderId())) {
            log.warn("Duplicate event detected, skipping reserveStock for orderId={}", order.orderId());
            return;
        }

        List<UUID> productIds = order.items().stream()
                .map(OrderEventItem::productId)
                .toList();

        Map<UUID, Stock> stockMap = stockRepository.findAllByProductIdInForUpdate(productIds)
                .stream()
                .collect(Collectors.toMap(Stock::getProductId, s -> s));

        UUID failedProductId = null;
        int requestedQty = 0;
        int availableQty = 0;

        for (OrderEventItem item : order.items()) {
            Stock stock = stockMap.get(item.productId());

            if (stock == null || stock.getAvailable() < item.quantity()) {
                failedProductId = item.productId();
                requestedQty = item.quantity();
                availableQty = stock != null ? stock.getAvailable() : 0;
                log.warn("Insufficient stock: productId={}, requested={}, available={}",
                        item.productId(), item.quantity(), availableQty);
                break;
            }
        }

        if (failedProductId != null) {
            publishOutbox(
                    EventType.STOCK_FAILED,
                    order.orderId().toString(),
                    new StockFailedPayload(order.orderId(), failedProductId, requestedQty, availableQty),
                    correlationId
            );
            return;
        }

        List<StockReservation> reservations = new ArrayList<>();
        for (OrderEventItem item : order.items()) {
            Stock stock = stockMap.get(item.productId());
            stock.setReserved(stock.getReserved() + item.quantity());
            stockRepository.save(stock);

            reservations.add(StockReservation.builder()
                    .orderId(order.orderId())
                    .productId(item.productId())
                    .quantity(item.quantity())
                    .status(ReservationStatus.PENDING)
                    .correlationId(correlationId)
                    .build());
        }

        reservationRepository.saveAll(reservations);

        publishOutbox(
                EventType.STOCK_RESERVED,
                order.orderId().toString(),
                new StockReservedPayload(order.orderId()),
                correlationId
        );
        log.info("Stock reserved for orderId={}", order.orderId());
    }

    @Transactional
    public void releaseReservations(UUID orderId, String correlationId) {
        log.info("Releasing reservations for orderId={}, correlationId={}", orderId, correlationId);
        List<StockReservation> pending = reservationRepository
                .findAllByOrderIdAndStatus(orderId, ReservationStatus.PENDING);

        for (StockReservation reservation : pending) {
            int updated = stockRepository.decrementReserved(reservation.getProductId(), reservation.getQuantity());
            if (updated == 0) {
                log.warn("Could not decrement reserved for productId={}, qty={}, correlationId={} — already zero or product inactive",
                        reservation.getProductId(), reservation.getQuantity(), correlationId);
            }
            reservation.setStatus(ReservationStatus.RELEASED);
        }
        reservationRepository.saveAll(pending);
        log.info("Released {} reservations for orderId={}, correlationId={}", pending.size(), orderId, correlationId);
    }

    @Transactional
    public void confirmReservations(UUID orderId, String correlationId) {
        log.info("Confirming reservations for orderId={}, correlationId={}", orderId, correlationId);
        List<StockReservation> pending = reservationRepository
                .findAllByOrderIdAndStatus(orderId, ReservationStatus.PENDING);

        for (StockReservation reservation : pending) {
            int updated = stockRepository.decrementQuantityAndReserved(reservation.getProductId(), reservation.getQuantity());
            if (updated == 0) {
                log.warn("Could not decrement quantity/reserved for productId={}, qty={}, correlationId={} — insufficient stock or product inactive",
                        reservation.getProductId(), reservation.getQuantity(), correlationId);
            }
            reservation.setStatus(ReservationStatus.CONFIRMED);
        }
        reservationRepository.saveAll(pending);
        log.info("Confirmed {} reservations for orderId={}, correlationId={}", pending.size(), orderId, correlationId);
    }

    @Transactional
    public void publishStockFailed(UUID orderId, String correlationId) {
        publishOutbox(
                EventType.STOCK_FAILED,
                orderId.toString(),
                new StockFailedPayload(orderId, null, 0, 0),
                correlationId
        );
        log.warn("Published STOCK_FAILED after exhausted retries: orderId={}", orderId);
    }

    private void publishOutbox(EventType eventType, String aggregateId,
                               Object payload, String correlationId) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(AGGREGATE_TYPE)
                    .aggregateId(aggregateId)
                    .eventType(eventType.name())
                    .payload(objectMapper.writeValueAsString(payload))
                    .correlationId(correlationId)
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for eventType={}", eventType, e);
            throw new RuntimeException("Outbox serialization failed", e);
        }
    }
}
