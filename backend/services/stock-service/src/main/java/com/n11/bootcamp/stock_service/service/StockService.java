package com.n11.bootcamp.stock_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.stock_service.dto.request.CreateStockRequest;
import com.n11.bootcamp.stock_service.dto.request.UpdateStockRequest;
import com.n11.bootcamp.stock_service.dto.response.ReservationResponse;
import com.n11.bootcamp.stock_service.dto.response.StockResponse;
import com.n11.bootcamp.stock_service.entity.Inventory;
import com.n11.bootcamp.stock_service.entity.OutboxEvent;
import com.n11.bootcamp.stock_service.entity.ReservationStatus;
import com.n11.bootcamp.stock_service.entity.StockReservation;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.order.OrderCreatedPayload;
import com.n11.bootcamp.common_lib.event.order.OrderEventItem;
import com.n11.bootcamp.stock_service.exception.InvalidStockQuantityException;
import com.n11.bootcamp.stock_service.exception.StockAlreadyExistsException;
import com.n11.bootcamp.stock_service.exception.StockNotFoundException;
import com.n11.bootcamp.stock_service.mapper.StockMapper;
import com.n11.bootcamp.stock_service.repository.StockRepository;
import com.n11.bootcamp.stock_service.repository.OutboxEventRepository;
import com.n11.bootcamp.stock_service.repository.StockReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final StockReservationRepository reservationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StockMapper stockMapper;
    private final ObjectMapper objectMapper;

    public StockService(StockRepository stockRepository, StockReservationRepository reservationRepository, OutboxEventRepository outboxEventRepository, StockMapper stockMapper, ObjectMapper objectMapper) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.stockMapper = stockMapper;
        this.objectMapper = objectMapper;
    }

    public List<StockResponse> getAll() {
        return stockRepository.findAllByIsActiveTrue()
                .stream()
                .map(stockMapper::toResponse)
                .toList();
    }

    public StockResponse getByProductId(UUID productId) {
        Inventory inventory = stockRepository.findByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new StockNotFoundException(productId));
        return stockMapper.toResponse(inventory);
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
        Inventory inventory = Inventory.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .reserved(0)
                .build();
        Inventory saved = stockRepository.save(inventory);
        log.info("Stock created: id={}, productId={}", saved.getId(), saved.getProductId());
        return stockMapper.toResponse(saved);
    }

    @Transactional
    public StockResponse updateStock(UUID productId, UpdateStockRequest request) {
        log.info("Updating stock: productId={}, newQuantity={}", productId, request.quantity());
        Inventory inventory = stockRepository.findByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new StockNotFoundException(productId));
        if (request.quantity() < inventory.getReserved()) {
            throw new InvalidStockQuantityException(productId, request.quantity(), inventory.getReserved());
        }
        inventory.setQuantity(request.quantity());
        Inventory saved = stockRepository.save(inventory);
        log.info("Stock updated: productId={}, quantity={}", productId, saved.getQuantity());
        return stockMapper.toResponse(saved);
    }

    @Transactional
    public void deleteStock(UUID productId) {
        log.info("Deleting stock (soft): productId={}", productId);
        Inventory inventory = stockRepository.findByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new StockNotFoundException(productId));
        inventory.setActive(false);
        stockRepository.save(inventory);
    }

    @Transactional
    public void reserveStock(OrderCreatedPayload order) {
        log.info("Reserving stock for orderId={}, correlationId={}", order.orderId(), order.correlationId());

        //TODO: change processed_events table
        if (reservationRepository.existsByOrderId(order.orderId())) {
            log.warn("Duplicate event detected, skipping reserveStock for orderId={}", order.orderId());
            return;
        }

        List<UUID> productIds = order.items().stream()
                .map(OrderEventItem::productId)
                .toList();

        Map<UUID, Inventory> inventoryMap = stockRepository.findAllByProductIdInForUpdate(productIds)
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i));

        UUID failedProductId = null;
        int requestedQty = 0;
        int availableQty = 0;

        for (OrderEventItem item : order.items()) {
            Inventory inventory = inventoryMap.get(item.productId());

            if (inventory == null || inventory.getAvailable() < item.quantity()) {
                failedProductId = item.productId();
                requestedQty = item.quantity();
                availableQty = inventory != null ? inventory.getAvailable() : 0;
                log.warn("Insufficient stock: productId={}, requested={}, available={}",
                        item.productId(), item.quantity(), availableQty);
                break;
            }
        }

        if (failedProductId != null) {
            publishOutbox(
                    EventType.INVENTORY_FAILED,
                    order.orderId().toString(),
                    buildInventoryFailedPayload(order.orderId(), failedProductId, requestedQty, availableQty, order.correlationId()),
                    order.correlationId()
            );
            return;
        }

        List<StockReservation> reservations = new ArrayList<>();
        for (OrderEventItem item : order.items()) {
            Inventory inventory = inventoryMap.get(item.productId());
            inventory.setReserved(inventory.getReserved() + item.quantity());
            stockRepository.save(inventory);

            reservations.add(StockReservation.builder()
                    .orderId(order.orderId())
                    .productId(item.productId())
                    .quantity(item.quantity())
                    .status(ReservationStatus.PENDING)
                    .correlationId(order.correlationId())
                    .build());
        }

        reservationRepository.saveAll(reservations);

        publishOutbox(
                EventType.INVENTORY_RESERVED,
                order.orderId().toString(),
                buildInventoryReservedPayload(order.orderId(), order.correlationId()),
                order.correlationId()
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

    private static final String AGGREGATE_TYPE = "inventory";

    @Transactional
    public void publishInventoryFailed(UUID orderId, String correlationId) {
        publishOutbox(
                EventType.INVENTORY_FAILED,
                orderId.toString(),
                buildInventoryFailedPayload(orderId, null, 0, 0, correlationId),
                correlationId
        );
        log.warn("Published INVENTORY_FAILED after exhausted retries: orderId={}", orderId);
    }

    private void publishOutbox(EventType eventType, String aggregateId,
                               Map<String, Object> payload, String correlationId) {
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

    private Map<String, Object> buildInventoryReservedPayload(UUID orderId, String correlationId) {
        return Map.of(
                "eventType", EventType.INVENTORY_RESERVED.name(),
                "orderId", orderId.toString(),
                "correlationId", correlationId != null ? correlationId : ""
        );
    }

    private Map<String, Object> buildInventoryFailedPayload(UUID orderId, UUID productId,
                                                            int requested, int available,
                                                            String correlationId) {
        return Map.of(
                "eventType", EventType.INVENTORY_FAILED.name(),
                "orderId", orderId.toString(),
                "failedProductId", productId != null ? productId.toString() : "",
                "requested", requested,
                "available", available,
                "correlationId", correlationId != null ? correlationId : ""
        );
    }
}
