package com.n11.bootcamp.payment_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.event.AggregateType;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.order.OrderCancelledPayload;
import com.n11.bootcamp.common_lib.event.order.OrderCreatedPayload;
import com.n11.bootcamp.common_lib.event.order.OrderEventItem;
import com.n11.bootcamp.common_lib.event.payment.PaymentEventPayload;
import com.n11.bootcamp.payment_service.dto.response.PaymentResponse;
import com.n11.bootcamp.payment_service.entity.OutboxEvent;
import com.n11.bootcamp.payment_service.entity.Payment;
import com.n11.bootcamp.payment_service.entity.PaymentStatus;
import com.n11.bootcamp.payment_service.exception.PaymentNotFoundException;
import com.n11.bootcamp.payment_service.gateway.PaymentGateway;
import com.n11.bootcamp.payment_service.gateway.PaymentGatewayRequest;
import com.n11.bootcamp.payment_service.gateway.PaymentGatewayResult;
import com.n11.bootcamp.payment_service.mapper.PaymentMapper;
import com.n11.bootcamp.payment_service.repository.OutboxEventRepository;
import com.n11.bootcamp.payment_service.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final String AGGREGATE_TYPE = AggregateType.PAYMENT;
    private static final String DEFAULT_CURRENCY = "TRY";

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxEventRepository outboxEventRepository,
                          PaymentGateway paymentGateway,
                          PaymentMapper paymentMapper,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentGateway = paymentGateway;
        this.paymentMapper = paymentMapper;
        this.objectMapper = objectMapper;
    }

    public PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderIdAndIsActiveTrue(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
        return paymentMapper.toResponse(payment);
    }

    public Page<PaymentResponse> listByUser(UUID userId, Pageable pageable) {
        return paymentRepository
                .findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId, pageable)
                .map(paymentMapper::toResponse);
    }

    @Transactional
    public void registerPendingPayment(OrderCreatedPayload order, String correlationId) {
        Payment payment = paymentRepository.findByOrderIdAndIsActiveTrue(order.orderId())
                .orElseGet(() -> Payment.builder().orderId(order.orderId()).build());

        if (payment.getUserId() != null) {
            log.warn("Duplicate ORDER_CREATED detected, skipping field overwrite for orderId={}",
                    order.orderId());
            return;
        }

        payment.setUserId(order.userId());
        payment.setAmount(computeAmount(order.items()));
        payment.setCurrency(DEFAULT_CURRENCY);
        payment.setCorrelationId(correlationId);

        // Snapshot buyer + shipping for self-contained Iyzico request + audit + refund
        if (order.buyer() != null) {
            payment.setBuyerFirstName(order.buyer().firstName());
            payment.setBuyerLastName(order.buyer().lastName());
            payment.setBuyerEmail(order.buyer().email());
            payment.setBuyerPhone(order.buyer().phone());
            payment.setBuyerIdentityNumber(order.buyer().identityNumber());
            payment.setBuyerIp(order.buyer().ip());
        }
        if (order.shippingAddress() != null) {
            payment.setShippingContactName(order.shippingAddress().contactName());
            payment.setShippingAddress(order.shippingAddress().fullAddress());
            payment.setShippingCity(order.shippingAddress().city());
            payment.setShippingDistrict(order.shippingAddress().district());
            payment.setShippingCountry(order.shippingAddress().country());
            payment.setShippingZipCode(order.shippingAddress().zipCode());
            payment.setShippingPhone(order.shippingAddress().phone());
        }

        payment = paymentRepository.save(payment);

        log.info("Order side recorded: paymentId={}, orderId={}, amount={} {}, stockConfirmed={}, correlationId={}",
                payment.getId(), order.orderId(), payment.getAmount(), payment.getCurrency(),
                payment.isStockConfirmed(), correlationId);

        maybeProcessPayment(payment, correlationId);
    }

    @Transactional
    public void confirmStockReservation(UUID orderId, String correlationId) {
        Payment payment = paymentRepository.findByOrderIdAndIsActiveTrue(orderId)
                .orElseGet(() -> Payment.builder()
                        .orderId(orderId)
                        .correlationId(correlationId)
                        .build());

        if (payment.isStockConfirmed()) {
            log.warn("Duplicate STOCK_RESERVED detected, skipping for orderId={}", orderId);
            return;
        }

        payment.setStockConfirmed(true);
        if (payment.getCorrelationId() == null) {
            payment.setCorrelationId(correlationId);
        }
        payment = paymentRepository.save(payment);

        log.info("Stock side recorded: paymentId={}, orderId={}, hasOrderInfo={}, correlationId={}",
                payment.getId(), orderId, payment.getUserId() != null, correlationId);

        maybeProcessPayment(payment, correlationId);
    }

    @Transactional
    public void handleOrderCancelled(OrderCancelledPayload cancelled, String correlationId) {
        UUID orderId = cancelled.orderId();
        Payment payment = paymentRepository.findByOrderIdAndIsActiveTrue(orderId).orElse(null);
        if (payment == null) {
            log.info("ORDER_CANCELLED arrived but no Payment exists for orderId={} — likely user cancelled before any payment event reached us",
                    orderId);
            return;
        }

        switch (payment.getStatus()) {
            case PENDING -> {
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setErrorCode("ORDER_CANCELLED");
                payment.setErrorMessage("Cancelled by user before charge: reason=" + cancelled.cancelReason());
                paymentRepository.save(payment);
                log.info("Payment CANCELLED before charge: orderId={}, reason={}", orderId, cancelled.cancelReason());
            }
            case COMPLETED -> refundCompletedPayment(payment, cancelled, correlationId);
            case FAILED, CANCELLED, REFUNDED ->
                log.info("ORDER_CANCELLED received but Payment is in terminal state: orderId={}, status={} — no-op",
                        orderId, payment.getStatus());
        }
    }

    private void refundCompletedPayment(Payment payment, OrderCancelledPayload cancelled, String correlationId) {
        UUID orderId = payment.getOrderId();
        log.warn("Refund triggered: orderId={}, providerPaymentId={}, amount={} {}, reason={}",
                orderId, payment.getProviderPaymentId(), payment.getAmount(), payment.getCurrency(),
                cancelled.cancelReason());

        PaymentGatewayResult refund = paymentGateway.refund(
                orderId, payment.getProviderPaymentId(), payment.getAmount(), correlationId);

        if (refund.success()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setErrorCode("ORDER_CANCELLED");
            payment.setErrorMessage("Refunded after user cancellation: reason=" + cancelled.cancelReason());
            paymentRepository.save(payment);

            publishOutbox(
                    EventType.PAYMENT_REFUNDED,
                    orderId.toString(),
                    new PaymentEventPayload(orderId, payment.getId(), PaymentStatus.REFUNDED.name(),
                            "User cancellation: " + cancelled.cancelReason()),
                    correlationId
            );
            log.info("Payment REFUNDED: orderId={}, providerPaymentId={}", orderId, payment.getProviderPaymentId());
        } else {
            log.error("REFUND FAILED — manual intervention required: orderId={}, errorCode={}, errorMessage={}",
                    orderId, refund.errorCode(), refund.errorMessage());
        }
    }

    @Transactional
    public void publishPaymentFailed(UUID orderId, String reason, String correlationId) {
        Payment payment = paymentRepository.findByOrderIdAndIsActiveTrue(orderId).orElse(null);

        if (payment != null && payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Skipping duplicate PAYMENT_FAILED — payment already terminal: orderId={}, status={}",
                    orderId, payment.getStatus());
            return;
        }

        if (payment != null) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorCode("RETRIES_EXHAUSTED");
            payment.setErrorMessage(reason);
            paymentRepository.save(payment);
        }

        publishOutbox(
                EventType.PAYMENT_FAILED,
                orderId.toString(),
                new PaymentEventPayload(orderId,
                        payment != null ? payment.getId() : null,
                        PaymentStatus.FAILED.name(), reason),
                correlationId
        );
        log.warn("Compensating PAYMENT_FAILED published: orderId={}, reason={}", orderId, reason);
    }

    private void maybeProcessPayment(Payment payment, String correlationId) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.debug("Payment in terminal/non-pending state, skipping charge: orderId={}, status={}",
                    payment.getOrderId(), payment.getStatus());
            return;
        }
        if (!payment.isStockConfirmed()) {
            log.debug("Rendezvous incomplete — waiting for STOCK_RESERVED: orderId={}", payment.getOrderId());
            return;
        }
        if (payment.getUserId() == null || payment.getAmount() == null) {
            log.debug("Rendezvous incomplete — waiting for ORDER_CREATED: orderId={}", payment.getOrderId());
            return;
        }
        chargeAndPublish(payment, correlationId);
    }

    private void chargeAndPublish(Payment payment, String correlationId) {
        log.info("Rendezvous complete, charging: orderId={}, amount={} {}, correlationId={}",
                payment.getOrderId(), payment.getAmount(), payment.getCurrency(), correlationId);

        PaymentGatewayResult result = paymentGateway.charge(buildGatewayRequest(payment, correlationId));

        if (result.success()) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProviderPaymentId(result.providerPaymentId());
            paymentRepository.save(payment);

            publishOutbox(
                    EventType.PAYMENT_COMPLETED,
                    payment.getOrderId().toString(),
                    new PaymentEventPayload(payment.getOrderId(), payment.getId(),
                            PaymentStatus.COMPLETED.name(), null),
                    correlationId
            );
            log.info("Payment COMPLETED: orderId={}, providerPaymentId={}",
                    payment.getOrderId(), result.providerPaymentId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorCode(result.errorCode());
            payment.setErrorMessage(result.errorMessage());
            paymentRepository.save(payment);

            publishOutbox(
                    EventType.PAYMENT_FAILED,
                    payment.getOrderId().toString(),
                    new PaymentEventPayload(payment.getOrderId(), payment.getId(),
                            PaymentStatus.FAILED.name(), result.errorMessage()),
                    correlationId
            );
            log.warn("Payment FAILED: orderId={}, errorCode={}, errorMessage={}",
                    payment.getOrderId(), result.errorCode(), result.errorMessage());
        }
    }

    private PaymentGatewayRequest buildGatewayRequest(Payment payment, String correlationId) {
        PaymentGatewayRequest.Buyer buyer = new PaymentGatewayRequest.Buyer(
                payment.getBuyerFirstName(),
                payment.getBuyerLastName(),
                payment.getBuyerEmail(),
                payment.getBuyerPhone(),
                payment.getBuyerIdentityNumber(),
                payment.getBuyerIp()
        );
        PaymentGatewayRequest.Address address = new PaymentGatewayRequest.Address(
                payment.getShippingContactName(),
                payment.getShippingAddress(),
                payment.getShippingCity(),
                payment.getShippingDistrict(),
                payment.getShippingCountry(),
                payment.getShippingZipCode(),
                payment.getShippingPhone()
        );
        return new PaymentGatewayRequest(
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                List.of(new PaymentGatewayRequest.Item(payment.getOrderId(), 1, payment.getAmount())),
                buyer,
                address,
                correlationId
        );
    }

    private BigDecimal computeAmount(List<OrderEventItem> items) {
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
            throw new IllegalStateException("Outbox serialization failed", e);
        }
    }
}
