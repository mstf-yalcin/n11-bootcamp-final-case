package com.n11.bootcamp.payment_service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.order.OrderCreatedPayload;
import com.n11.bootcamp.common_lib.event.stock.StockReservedPayload;
import com.n11.bootcamp.payment_service.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false
        ));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> orderKafkaListenerContainerFactory(
            DefaultErrorHandler orderErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setRecordMessageConverter(new StringJsonMessageConverter());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setAckDiscarded(true);
        factory.setCommonErrorHandler(orderErrorHandler);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> stockKafkaListenerContainerFactory(
            DefaultErrorHandler stockErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setRecordMessageConverter(new StringJsonMessageConverter());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setAckDiscarded(true);
        factory.setCommonErrorHandler(stockErrorHandler);
        return factory;
    }

    /**
     * order.events recoverer:
     *   - ORDER_CREATED retries exhausted -> publish PAYMENT_FAILED so the saga compensates
     *     (order-service cancels the order, stock-service releases reservation if any).
     *   - ORDER_CANCELLED retries exhausted -> log only; the cancel itself is the compensation,
     *     no further compensation is meaningful from payment side.
     */
    @Bean
    public DefaultErrorHandler orderErrorHandler(PaymentService paymentService, ObjectMapper objectMapper) {
        ExponentialBackOff backOff = new ExponentialBackOff(3000L, 2.0);
        backOff.setMaxAttempts(3);

        DefaultErrorHandler handler = new DefaultErrorHandler((record, exception) -> {
            String eventType = extractHeader(record, "eventType");
            String correlationId = extractHeader(record, "correlationId");
            log.error("All retries exhausted for order event: eventType={}, topic={}, offset={}, key={}, exception={}",
                    eventType, record.topic(), record.offset(), record.key(), exception.getMessage());

            if (EventType.ORDER_CREATED.name().equals(eventType)) {
                UUID orderId = parseOrderIdFromMessage(record, objectMapper);
                if (orderId == null) orderId = parseOrderIdFromKey(record.key());
                if (orderId != null) {
                    paymentService.publishPaymentFailed(orderId,
                            "ORDER_CREATED retries exhausted: " + exception.getMessage(), correlationId);
                } else {
                    log.error("Cannot extract orderId from failed ORDER_CREATED — manual reconciliation required");
                }
            } else {
                log.error("Manual intervention required: order event {} stuck after retries (offset={}, key={})",
                        eventType, record.offset(), record.key());
            }
        }, backOff);

        handler.addNotRetryableExceptions(
                JsonProcessingException.class,
                IllegalArgumentException.class,
                NullPointerException.class,
                ClassCastException.class
        );
        return handler;
    }

    /**
     * stock.events recoverer: STOCK_RESERVED retries exhausted (e.g. gateway down, DB error)
     * -> publish PAYMENT_FAILED so the saga compensates (stock release + order cancel).
     */
    @Bean
    public DefaultErrorHandler stockErrorHandler(PaymentService paymentService, ObjectMapper objectMapper) {
        ExponentialBackOff backOff = new ExponentialBackOff(3000L, 2.0);
        backOff.setMaxAttempts(3);

        DefaultErrorHandler handler = new DefaultErrorHandler((record, exception) -> {
            String eventType = extractHeader(record, "eventType");
            String correlationId = extractHeader(record, "correlationId");
            log.error("All retries exhausted for stock event: eventType={}, topic={}, offset={}, exception={}",
                    eventType, record.topic(), record.offset(), exception.getMessage());

            if (EventType.STOCK_RESERVED.name().equals(eventType)) {
                UUID orderId = null;
                try {
                    StockReservedPayload payload = objectMapper.readValue(
                            record.value().toString(), StockReservedPayload.class);
                    orderId = payload.orderId();
                } catch (Exception e) {
                    log.error("Recoverer failed to parse STOCK_RESERVED payload, falling back to key", e);
                    orderId = parseOrderIdFromKey(record.key());
                }
                if (orderId != null) {
                    paymentService.publishPaymentFailed(orderId,
                            "STOCK_RESERVED retries exhausted: " + exception.getMessage(), correlationId);
                } else {
                    log.error("Cannot extract orderId from failed STOCK_RESERVED — manual reconciliation required");
                }
            }
        }, backOff);

        handler.addNotRetryableExceptions(
                JsonProcessingException.class,
                IllegalArgumentException.class,
                NullPointerException.class,
                ClassCastException.class
        );
        return handler;
    }

    @Bean
    public RecordFilterStrategy<String, String> orderEventFilter() {
        return record -> {
            String eventType = extractHeader(record, "eventType");
            return !EventType.ORDER_CREATED.name().equals(eventType)
                    && !EventType.ORDER_CANCELLED.name().equals(eventType);
        };
    }

    @Bean
    public RecordFilterStrategy<String, String> stockEventFilter() {
        return record -> !EventType.STOCK_RESERVED.name().equals(extractHeader(record, "eventType"));
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        var header = record.headers().lastHeader(headerName);
        return header != null ? new String(header.value()) : null;
    }

    private UUID parseOrderIdFromKey(Object key) {
        if (key == null) return null;
        try {
            return UUID.fromString(key.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private UUID parseOrderIdFromMessage(ConsumerRecord<?, ?> record, ObjectMapper objectMapper) {
        try {
            OrderCreatedPayload payload = objectMapper.readValue(
                    record.value().toString(), OrderCreatedPayload.class);
            return payload.orderId();
        } catch (Exception e) {
            return null;
        }
    }
}
