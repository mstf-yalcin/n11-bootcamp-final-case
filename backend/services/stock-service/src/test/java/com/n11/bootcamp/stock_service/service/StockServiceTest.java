package com.n11.bootcamp.stock_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.stock_service.dto.request.CreateStockRequest;
import com.n11.bootcamp.stock_service.dto.request.UpdateStockRequest;
import com.n11.bootcamp.stock_service.dto.response.StockResponse;
import com.n11.bootcamp.stock_service.entity.Inventory;
import com.n11.bootcamp.stock_service.entity.OutboxEvent;
import com.n11.bootcamp.stock_service.entity.ReservationStatus;
import com.n11.bootcamp.stock_service.entity.StockReservation;
import com.n11.bootcamp.common_lib.event.order.OrderCreatedPayload;
import com.n11.bootcamp.common_lib.event.order.OrderEventItem;
import com.n11.bootcamp.stock_service.exception.StockAlreadyExistsException;
import com.n11.bootcamp.stock_service.exception.StockNotFoundException;
import com.n11.bootcamp.stock_service.mapper.StockMapper;
import com.n11.bootcamp.stock_service.repository.StockRepository;
import com.n11.bootcamp.stock_service.repository.OutboxEventRepository;
import com.n11.bootcamp.stock_service.repository.StockReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockReservationRepository reservationRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private StockMapper stockMapper;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private StockService stockService;

    @Test
    void testCreateStock_when_productIdNew_returnsStockResponse() {
        UUID productId = UUID.randomUUID();
        CreateStockRequest request = new CreateStockRequest(productId, 100);
        Inventory savedInventory = Inventory.builder().productId(productId).quantity(100).reserved(0).build();
        StockResponse expected = new StockResponse(UUID.randomUUID(), productId, 100, 0, 100, null);

        when(stockRepository.existsByProductId(productId)).thenReturn(false);
        when(stockRepository.save(any())).thenReturn(savedInventory);
        when(stockMapper.toResponse(savedInventory)).thenReturn(expected);

        StockResponse result = stockService.createStock(request);

        assertThat(result).isEqualTo(expected);
        verify(stockRepository).save(any(Inventory.class));
    }

    @Test
    void testCreateStock_when_productAlreadyExists_throwsException() {
        UUID productId = UUID.randomUUID();
        CreateStockRequest request = new CreateStockRequest(productId, 100);

        when(stockRepository.existsByProductId(productId)).thenReturn(true);

        assertThatThrownBy(() -> stockService.createStock(request))
                .isInstanceOf(StockAlreadyExistsException.class);

        verify(stockRepository, never()).save(any());
    }

    @Test
    void testGetByProductId_when_stockNotFound_throwsException() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.getByProductId(productId))
                .isInstanceOf(StockNotFoundException.class);
    }

    @Test
    void testUpdateStock_when_stockExists_updatesQuantity() {
        UUID productId = UUID.randomUUID();
        Inventory inventory = Inventory.builder().productId(productId).quantity(50).reserved(5).build();
        UpdateStockRequest request = new UpdateStockRequest(200);
        StockResponse expected = new StockResponse(UUID.randomUUID(), productId, 200, 5, 195, null);

        when(stockRepository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.of(inventory));
        when(stockRepository.save(inventory)).thenReturn(inventory);
        when(stockMapper.toResponse(inventory)).thenReturn(expected);

        StockResponse result = stockService.updateStock(productId, request);

        assertThat(result.quantity()).isEqualTo(200);
        assertThat(inventory.getQuantity()).isEqualTo(200);
    }

    @Test
    void testReserveStock_when_stockAvailable_reservesAndPublishesInventoryReserved() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        Inventory inventory = Inventory.builder()
                .productId(productId).quantity(100).reserved(0).build();

        OrderCreatedPayload order = new OrderCreatedPayload(
                orderId, UUID.randomUUID(), correlationId,
                List.of(new OrderEventItem(productId, 5, BigDecimal.valueOf(50)))
        );

        when(stockRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));
        when(stockRepository.save(inventory)).thenReturn(inventory);
        when(reservationRepository.saveAll(anyList())).thenReturn(List.of());
        when(outboxEventRepository.save(any())).thenReturn(new OutboxEvent());

        stockService.reserveStock(order);

        assertThat(inventory.getReserved()).isEqualTo(5);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("INVENTORY_RESERVED");
        assertThat(captor.getValue().getAggregateType()).isEqualTo("inventory");
    }

    @Test
    void testReserveStock_when_stockInsufficient_publishesInventoryFailed() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Inventory inventory = Inventory.builder()
                .productId(productId).quantity(3).reserved(0).build();

        OrderCreatedPayload order = new OrderCreatedPayload(
                orderId, UUID.randomUUID(), "corr-1",
                List.of(new OrderEventItem(productId, 10, BigDecimal.valueOf(50)))
        );

        when(stockRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));
        when(outboxEventRepository.save(any())).thenReturn(new OutboxEvent());

        stockService.reserveStock(order);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("INVENTORY_FAILED");
        verify(reservationRepository, never()).saveAll(anyList());
    }

    @Test
    void testReserveStock_when_productNotInInventory_publishesInventoryFailed() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderCreatedPayload order = new OrderCreatedPayload(
                orderId, UUID.randomUUID(), "corr-2",
                List.of(new OrderEventItem(productId, 1, BigDecimal.valueOf(50)))
        );

        when(stockRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.empty());
        when(outboxEventRepository.save(any())).thenReturn(new OutboxEvent());

        stockService.reserveStock(order);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("INVENTORY_FAILED");
    }

    @Test
    void testReleaseReservations_when_pendingExist_releasesStockAndUpdatesStatus() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        StockReservation reservation = StockReservation.builder()
                .orderId(orderId).productId(productId).quantity(5).status(ReservationStatus.PENDING).build();

        Inventory inventory = Inventory.builder()
                .productId(productId).quantity(100).reserved(5).build();

        when(reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.PENDING))
                .thenReturn(List.of(reservation));
        when(stockRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));
        when(stockRepository.save(inventory)).thenReturn(inventory);
        when(reservationRepository.saveAll(anyList())).thenReturn(List.of());

        stockService.releaseReservations(orderId, "corr-1");

        assertThat(inventory.getReserved()).isEqualTo(0);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void testConfirmReservations_when_pendingExist_reducesStockAndUpdatesStatus() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        StockReservation reservation = StockReservation.builder()
                .orderId(orderId).productId(productId).quantity(3).status(ReservationStatus.PENDING).build();

        Inventory inventory = Inventory.builder()
                .productId(productId).quantity(100).reserved(3).build();

        when(reservationRepository.findAllByOrderIdAndStatus(orderId, ReservationStatus.PENDING))
                .thenReturn(List.of(reservation));
        when(stockRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));
        when(stockRepository.save(inventory)).thenReturn(inventory);
        when(reservationRepository.saveAll(anyList())).thenReturn(List.of());

        stockService.confirmReservations(orderId, "corr-1");

        assertThat(inventory.getQuantity()).isEqualTo(97);
        assertThat(inventory.getReserved()).isEqualTo(0);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void testDeleteStock_when_stockNotFound_throwsException() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.findByProductIdAndIsActiveTrue(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.deleteStock(productId))
                .isInstanceOf(StockNotFoundException.class);
    }
}
