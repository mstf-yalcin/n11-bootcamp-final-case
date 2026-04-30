package com.n11.bootcamp.stock_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.order.OrderCancelledPayload;
import com.n11.bootcamp.common_lib.event.order.OrderCreatedPayload;
import com.n11.bootcamp.common_lib.event.payment.PaymentEventPayload;
import com.n11.bootcamp.stock_service.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventConsumer {

    private static final String CORRELATION_ID_KEY = "correlationId";

    private final StockService stockService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(StockService stockService, ObjectMapper objectMapper) {
        this.stockService = stockService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order.events", groupId = "stock-service-order-consumer",
            containerFactory = "orderKafkaListenerContainerFactory", filter = "orderEventFilter")
    public void consumeOrderEvent(@Payload String message,
                                  @Header("eventType") String eventTypeHeader,
                                  @Header(name = "id", required = false) String eventId,
                                  @Header(name = "correlationId", required = false) String correlationId,
                                  Acknowledgment ack) throws Exception {
        try {
            bindCorrelation(correlationId);
            EventType type = EventType.valueOf(eventTypeHeader);
            switch (type) {
                case ORDER_CREATED -> {
                    OrderCreatedPayload payload = objectMapper.readValue(message, OrderCreatedPayload.class);
                    log.info("Received ORDER_CREATED: eventId={}, orderId={}, correlationId={}",
                            eventId, payload.orderId(), correlationId);
                    stockService.reserveStock(payload, correlationId);
                }
                case ORDER_CANCELLED -> {
                    OrderCancelledPayload payload = objectMapper.readValue(message, OrderCancelledPayload.class);
                    log.info("Received ORDER_CANCELLED: eventId={}, orderId={}, reason={}, correlationId={}",
                            eventId, payload.orderId(), payload.cancelReason(), correlationId);
                    stockService.releaseReservations(payload.orderId(), correlationId);
                }
                default -> log.debug("Ignoring order event: {}", eventTypeHeader);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order event: eventType={}", eventTypeHeader, e);
            throw e;
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    @KafkaListener(topics = "payment.events", groupId = "stock-service-payment-consumer",
            containerFactory = "paymentKafkaListenerContainerFactory", filter = "paymentEventFilter")
    public void consumePaymentEvent(@Payload String message,
                                    @Header("eventType") String eventTypeHeader,
                                    @Header(name = "id", required = false) String eventId,
                                    @Header(name = "correlationId", required = false) String correlationId,
                                    Acknowledgment ack) throws Exception {
        try {
            bindCorrelation(correlationId);
            EventType type = EventType.valueOf(eventTypeHeader);
            PaymentEventPayload payload = objectMapper.readValue(message, PaymentEventPayload.class);
            log.info("Received payment event: eventType={}, eventId={}, orderId={}, correlationId={}",
                    eventTypeHeader, eventId, payload.orderId(), correlationId);

            if (type == EventType.PAYMENT_FAILED) {
                stockService.releaseReservations(payload.orderId(), correlationId);
            } else {
                stockService.confirmReservations(payload.orderId(), correlationId);
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
