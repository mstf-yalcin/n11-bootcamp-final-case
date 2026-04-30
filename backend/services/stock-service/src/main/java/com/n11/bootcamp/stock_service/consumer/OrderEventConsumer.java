package com.n11.bootcamp.stock_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.order.OrderCreatedPayload;
import com.n11.bootcamp.common_lib.event.payment.PaymentEventPayload;
import com.n11.bootcamp.stock_service.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final StockService stockService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.events", groupId = "stock-service-order-consumer",
            containerFactory = "orderKafkaListenerContainerFactory", filter = "orderCreatedFilter")
    public void consumeOrderEvent(@Payload String message,
                                  @Header("eventType") String eventType,
                                  Acknowledgment ack) throws Exception {
        try {
            OrderCreatedPayload payload = objectMapper.readValue(message, OrderCreatedPayload.class);
            log.info("Received order event: eventType={}, eventId={}, orderId={}, correlationId={}",
                    eventType, payload.eventId(), payload.orderId(), payload.correlationId());
            stockService.reserveStock(payload);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order event: eventType={}", eventType, e);
            throw e;
        }
    }

    @KafkaListener(topics = "payment.events", groupId = "stock-service-payment-consumer",
            containerFactory = "paymentKafkaListenerContainerFactory", filter = "paymentEventFilter")
    public void consumePaymentEvent(@Payload String message,
                                    @Header("eventType") String eventType,
                                    Acknowledgment ack) throws Exception {
        try {
            EventType type = EventType.valueOf(eventType);
            PaymentEventPayload payload = objectMapper.readValue(message, PaymentEventPayload.class);
            log.info("Received payment event: eventType={}, eventId={}, orderId={}, correlationId={}",
                    eventType, payload.eventId(), payload.orderId(), payload.correlationId());

            if (type == EventType.PAYMENT_FAILED) {
                stockService.releaseReservations(payload.orderId(), payload.correlationId());
            } else {
                stockService.confirmReservations(payload.orderId(), payload.correlationId());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process payment event: eventType={}", eventType, e);
            throw e;
        }
    }
}
