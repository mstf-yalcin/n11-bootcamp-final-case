package com.n11.bootcamp.stock_service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.order.OrderCreatedPayload;
import com.n11.bootcamp.stock_service.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;

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
    public ConcurrentKafkaListenerContainerFactory<String, String> paymentKafkaListenerContainerFactory(
            DefaultErrorHandler paymentErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setRecordMessageConverter(new StringJsonMessageConverter());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setAckDiscarded(true);
        factory.setCommonErrorHandler(paymentErrorHandler);
        return factory;
    }

    @Bean
    public DefaultErrorHandler orderErrorHandler(StockService stockService, ObjectMapper objectMapper) {
        ExponentialBackOff backOff = new ExponentialBackOff(3000L, 2.0);
        backOff.setMaxAttempts(3);

        DefaultErrorHandler handler = new DefaultErrorHandler((record, exception) -> {
            String eventType = extractHeader(record, "eventType");
            String correlationId = extractHeader(record, "correlationId");
            log.error("All retries exhausted for order event: eventType={}, topic={}, offset={}, exception={}",
                    eventType, record.topic(), record.offset(), exception.getMessage());

            if (EventType.ORDER_CREATED.name().equals(eventType)) {
                try {
                    OrderCreatedPayload payload = objectMapper.readValue(
                            record.value().toString(), OrderCreatedPayload.class);
                    stockService.publishStockFailed(payload.orderId(), correlationId);
                    log.error("Published STOCK_FAILED for orderId={}", payload.orderId());
                } catch (Exception e) {
                    log.error("Recoverer failed to publish STOCK_FAILED for offset={}", record.offset(), e);
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

    @Bean
    public DefaultErrorHandler paymentErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        ExponentialBackOff backOff = new ExponentialBackOff(3000L, 2.0);
        backOff.setMaxAttempts(3);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> {
                    log.error("All retries exhausted for payment event: topic={}, offset={}, exception={}",
                            record.topic(), record.offset(), exception.getMessage());
                    return new TopicPartition(record.topic() + ".DLT", record.partition());
                });

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
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
    public RecordFilterStrategy<String, String> paymentEventFilter() {
        return record -> {
            String eventType = extractHeader(record, "eventType");
            return !EventType.PAYMENT_COMPLETED.name().equals(eventType)
                    && !EventType.PAYMENT_FAILED.name().equals(eventType);
        };
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        var header = record.headers().lastHeader(headerName);
        return header != null ? new String(header.value()) : null;
    }
}
