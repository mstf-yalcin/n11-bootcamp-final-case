package com.n11.bootcamp.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.order.CancelReason;
import com.n11.bootcamp.common_lib.event.order.OrderCancelledPayload;
import com.n11.bootcamp.common_lib.event.order.OrderCreatedPayload;
import com.n11.bootcamp.common_lib.event.order.OrderEventItem;
import com.n11.bootcamp.payment_service.entity.OutboxEvent;
import com.n11.bootcamp.payment_service.entity.Payment;
import com.n11.bootcamp.payment_service.entity.PaymentStatus;
import com.n11.bootcamp.payment_service.gateway.PaymentGateway;
import com.n11.bootcamp.payment_service.gateway.PaymentGatewayRequest;
import com.n11.bootcamp.payment_service.gateway.PaymentGatewayResult;
import com.n11.bootcamp.payment_service.mapper.PaymentMapper;
import com.n11.bootcamp.payment_service.repository.OutboxEventRepository;
import com.n11.bootcamp.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PaymentMapper paymentMapper;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentService paymentService;

    private static final String CORRELATION_ID = "corr-123";
    private UUID orderId;
    private UUID userId;
    private OrderCreatedPayload orderCreatedPayload;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        orderCreatedPayload = new OrderCreatedPayload(orderId, userId, List.of(
                // 2 * 50.00 = 100.00 total
                new OrderEventItem(UUID.randomUUID(), 2, new BigDecimal("50.00"))
        ));
    }

    @Test
    @DisplayName("ORDER_CREATED first → Payment(PENDING) created, no Iyzico call")
    void testRegisterPendingPayment_whenOrderArrivesFirst_createsPendingAndDoesNotCharge() {
        when(paymentRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.registerPendingPayment(orderCreatedPayload, CORRELATION_ID);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getCurrency()).isEqualTo("TRY");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.isStockConfirmed()).isFalse();

        verify(paymentGateway, never()).charge(any(PaymentGatewayRequest.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("ORDER_CREATED arrives after STOCK_RESERVED → rendezvous completes, Iyzico charged, PAYMENT_COMPLETED emitted")
    void testRegisterPendingPayment_whenStockAlreadyConfirmed_chargesAndPublishesCompleted() {
        Payment stub = Payment.builder()
                .orderId(orderId)
                .stockConfirmed(true)
                .build();
        when(paymentRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(stub));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(PaymentGatewayRequest.class)))
                .thenReturn(PaymentGatewayResult.ok("iyz_pay_42"));

        paymentService.registerPendingPayment(orderCreatedPayload, CORRELATION_ID);

        verify(paymentGateway).charge(any(PaymentGatewayRequest.class));
        assertThat(stub.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(stub.getProviderPaymentId()).isEqualTo("iyz_pay_42");

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.PAYMENT_COMPLETED.name());
        assertThat(outboxCaptor.getValue().getAggregateId()).isEqualTo(orderId.toString());
    }

    @Test
    @DisplayName("STOCK_RESERVED first → stub Payment(stockConfirmed=true) created, no Iyzico call")
    void testConfirmStockReservation_whenStockArrivesFirst_createsStubAndDoesNotCharge() {
        when(paymentRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.confirmStockReservation(orderId, CORRELATION_ID);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.isStockConfirmed()).isTrue();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getAmount()).isNull();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);

        verify(paymentGateway, never()).charge(any(PaymentGatewayRequest.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("STOCK_RESERVED arrives after ORDER_CREATED → rendezvous completes, Iyzico charged, PAYMENT_COMPLETED emitted")
    void testConfirmStockReservation_whenOrderAlreadyReceived_chargesAndPublishesCompleted() {
        Payment existing = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .build();
        when(paymentRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(PaymentGatewayRequest.class)))
                .thenReturn(PaymentGatewayResult.ok("iyz_pay_99"));

        paymentService.confirmStockReservation(orderId, CORRELATION_ID);

        verify(paymentGateway).charge(any(PaymentGatewayRequest.class));
        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(existing.isStockConfirmed()).isTrue();
        assertThat(existing.getProviderPaymentId()).isEqualTo("iyz_pay_99");

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.PAYMENT_COMPLETED.name());
    }

    @Test
    @DisplayName("Iyzico FAIL → Payment FAILED + PAYMENT_FAILED outbox")
    void testConfirmStockReservation_whenIyzicoFails_marksFailedAndPublishesPaymentFailed() {
        Payment existing = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .build();
        when(paymentRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(PaymentGatewayRequest.class)))
                .thenReturn(PaymentGatewayResult.fail("INSUFFICIENT_FUNDS", "Card declined"));

        paymentService.confirmStockReservation(orderId, CORRELATION_ID);

        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(existing.getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(existing.getErrorMessage()).isEqualTo("Card declined");

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.PAYMENT_FAILED.name());
    }

    @Test
    @DisplayName("ORDER_CANCELLED + Payment PENDING → CANCELLED, no Iyzico call, no outbox")
    void testHandleOrderCancelled_whenPending_marksCancelledWithoutCallingGateway() {
        Payment existing = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCancelledPayload cancelled = new OrderCancelledPayload(
                orderId, userId, CancelReason.USER_CANCELLED.name());
        paymentService.handleOrderCancelled(cancelled, CORRELATION_ID);

        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(existing.getErrorCode()).isEqualTo("ORDER_CANCELLED");
        verify(paymentGateway, never()).refund(any(), any(), any(), any());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("ORDER_CANCELLED + Payment COMPLETED → Iyzico Cancel + REFUNDED + PAYMENT_REFUNDED outbox")
    void testHandleOrderCancelled_whenCompleted_refundsAndPublishesPaymentRefunded() {
        Payment existing = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .status(PaymentStatus.COMPLETED)
                .providerPaymentId("iyz_pay_42")
                .build();
        when(paymentRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.refund(eq(orderId), eq("iyz_pay_42"), any(BigDecimal.class), eq(CORRELATION_ID)))
                .thenReturn(PaymentGatewayResult.ok("iyz_pay_42"));

        OrderCancelledPayload cancelled = new OrderCancelledPayload(
                orderId, userId, CancelReason.USER_CANCELLED.name());
        paymentService.handleOrderCancelled(cancelled, CORRELATION_ID);

        verify(paymentGateway).refund(eq(orderId), eq("iyz_pay_42"), any(BigDecimal.class), eq(CORRELATION_ID));
        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.PAYMENT_REFUNDED.name());
    }

    @Test
    @DisplayName("publishPaymentFailed → marks PENDING Payment FAILED + PAYMENT_FAILED outbox")
    void testPublishPaymentFailed_whenPending_marksFailedAndPublishesOutbox() {
        Payment existing = Payment.builder()
                .orderId(orderId)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.publishPaymentFailed(orderId, "Retries exhausted", CORRELATION_ID);

        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(existing.getErrorCode()).isEqualTo("RETRIES_EXHAUSTED");
        assertThat(existing.getErrorMessage()).isEqualTo("Retries exhausted");

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.PAYMENT_FAILED.name());
        assertThat(outboxCaptor.getValue().getAggregateId()).isEqualTo(orderId.toString());
    }
}
