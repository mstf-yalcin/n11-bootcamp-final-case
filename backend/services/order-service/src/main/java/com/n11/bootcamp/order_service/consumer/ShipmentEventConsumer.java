package com.n11.bootcamp.order_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.event.shipment.ShipmentCreatedPayload;
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
public class ShipmentEventConsumer {

    private static final String CORRELATION_ID_KEY = "correlationId";

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public ShipmentEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "shipment.events",
            groupId = "order-service-shipment-consumer",
            containerFactory = "shipmentKafkaListenerContainerFactory",
            filter = "shipmentEventFilter"
    )
    public void consume(@Payload String message,
                        @Header("eventType") String eventTypeHeader,
                        @Header(name = "id", required = false) String eventId,
                        @Header(name = "correlationId", required = false) String correlationId,
                        Acknowledgment ack) throws Exception {
        try {
            bindCorrelation(correlationId);
            ShipmentCreatedPayload payload = objectMapper.readValue(message, ShipmentCreatedPayload.class);
            log.info("Received SHIPMENT_CREATED: eventId={}, orderId={}, shipmentId={}, trackingNumber={}, correlationId={}",
                    eventId, payload.orderId(), payload.shipmentId(),
                    payload.trackingNumber(), correlationId);

            orderService.handleShipmentCreated(payload.orderId(), correlationId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process shipment event: eventType={}", eventTypeHeader, e);
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
