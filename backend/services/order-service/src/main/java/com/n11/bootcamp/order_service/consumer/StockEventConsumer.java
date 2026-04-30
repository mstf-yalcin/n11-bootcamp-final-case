package com.n11.bootcamp.order_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.stock.StockFailedPayload;
import com.n11.bootcamp.common_lib.event.stock.StockReservedPayload;
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
public class StockEventConsumer {

    private static final String CORRELATION_ID_KEY = "correlationId";

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public StockEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "stock.events",
            groupId = "order-service-stock-consumer",
            containerFactory = "stockKafkaListenerContainerFactory",
            filter = "stockEventFilter"
    )
    public void consume(@Payload String message,
                        @Header("eventType") String eventTypeHeader,
                        @Header(name = "id", required = false) String eventId,
                        @Header(name = "correlationId", required = false) String correlationId,
                        Acknowledgment ack) throws Exception {
        try {
            bindCorrelation(correlationId);
            EventType type = EventType.valueOf(eventTypeHeader);
            switch (type) {
                case STOCK_RESERVED -> {
                    StockReservedPayload payload = objectMapper.readValue(message, StockReservedPayload.class);
                    log.info("Received STOCK_RESERVED: eventId={}, orderId={}, correlationId={}",
                            eventId, payload.orderId(), correlationId);
                    orderService.handleStockReserved(payload.orderId(), correlationId);
                }
                case STOCK_FAILED -> {
                    StockFailedPayload payload = objectMapper.readValue(message, StockFailedPayload.class);
                    log.warn("Received STOCK_FAILED: eventId={}, orderId={}, failedProductId={}, requested={}, available={}, correlationId={}",
                            eventId, payload.orderId(), payload.failedProductId(),
                            payload.requested(), payload.available(), correlationId);
                    orderService.handleStockFailed(payload.orderId(), correlationId);
                }
                default -> log.debug("Ignoring stock event: {}", eventTypeHeader);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process stock event: eventType={}", eventTypeHeader, e);
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
