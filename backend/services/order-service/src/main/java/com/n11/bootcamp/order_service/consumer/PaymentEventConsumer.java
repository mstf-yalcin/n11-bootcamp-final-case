package com.n11.bootcamp.order_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.payment.PaymentEventPayload;
import com.n11.bootcamp.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventConsumer {

    private static final String CORRELATION_ID_KEY = "correlationId";

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "payment.events",
            groupId = "order-service-payment-consumer",
            containerFactory = "paymentKafkaListenerContainerFactory",
            filter = "paymentEventFilter"
    )
    public void consume(@Payload String message,
                        @Header("eventType") String eventTypeHeader,
                        @Header(name = "id", required = false) String eventId,
                        @Header(name = "correlationId", required = false) String correlationId,
                        Acknowledgment ack) throws Exception {
        try {
            bindCorrelation(correlationId);
            EventType type = EventType.valueOf(eventTypeHeader);
            PaymentEventPayload payload = objectMapper.readValue(message, PaymentEventPayload.class);

            log.info("Received {}: eventId={}, orderId={}, paymentId={}, status={}, correlationId={}",
                    type, eventId, payload.orderId(), payload.paymentId(),
                    payload.status(), correlationId);

            switch (type) {
                case PAYMENT_COMPLETED -> orderService.handlePaymentCompleted(
                        payload.orderId(), correlationId);
                case PAYMENT_FAILED -> orderService.handlePaymentFailed(
                        payload.orderId(), correlationId);
                default -> log.debug("Ignoring payment event: {}", eventTypeHeader);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process payment event: eventType={}", eventTypeHeader, e);
            throw e;
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    private void bindCorrelation(String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }
}
