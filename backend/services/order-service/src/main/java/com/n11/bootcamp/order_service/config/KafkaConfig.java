package com.n11.bootcamp.order_service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.n11.bootcamp.common_lib.event.EventType;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;
import java.util.Set;

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
    public ConcurrentKafkaListenerContainerFactory<String, String> stockKafkaListenerContainerFactory(
            DefaultErrorHandler stockErrorHandler) {
        return buildFactory(stockErrorHandler);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> paymentKafkaListenerContainerFactory(
            DefaultErrorHandler paymentErrorHandler) {
        return buildFactory(paymentErrorHandler);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> shipmentKafkaListenerContainerFactory(
            DefaultErrorHandler shipmentErrorHandler) {
        return buildFactory(shipmentErrorHandler);
    }

    private ConcurrentKafkaListenerContainerFactory<String, String> buildFactory(DefaultErrorHandler handler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setRecordMessageConverter(new StringJsonMessageConverter());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setAckDiscarded(true);
        factory.setCommonErrorHandler(handler);
        return factory;
    }

    @Bean
    public DefaultErrorHandler stockErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        return buildDltErrorHandler(kafkaTemplate, "stock");
    }

    @Bean
    public DefaultErrorHandler paymentErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        return buildDltErrorHandler(kafkaTemplate, "payment");
    }

    @Bean
    public DefaultErrorHandler shipmentErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        return buildDltErrorHandler(kafkaTemplate, "shipment");
    }

    private DefaultErrorHandler buildDltErrorHandler(KafkaTemplate<String, String> kafkaTemplate, String topicPrefix) {
        ExponentialBackOff backOff = new ExponentialBackOff(3000L, 2.0);
        backOff.setMaxAttempts(3);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> {
                    log.error("All retries exhausted for {} event: topic={}, offset={}, error={}",
                            topicPrefix, record.topic(), record.offset(), exception.getMessage());
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
    public RecordFilterStrategy<String, String> stockEventFilter() {
        return record -> !matchesAny(record, Set.of(
                EventType.STOCK_RESERVED.name(),
                EventType.STOCK_FAILED.name()
        ));
    }

    @Bean
    public RecordFilterStrategy<String, String> paymentEventFilter() {
        return record -> !matchesAny(record, Set.of(
                EventType.PAYMENT_COMPLETED.name(),
                EventType.PAYMENT_FAILED.name()
        ));
    }

    @Bean
    public RecordFilterStrategy<String, String> shipmentEventFilter() {
        return record -> !matchesAny(record, Set.of(
                EventType.SHIPMENT_CREATED.name()
        ));
    }

    private boolean matchesAny(ConsumerRecord<?, ?> record, Set<String> wanted) {
        return wanted.contains(extractHeader(record, "eventType"));
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        var header = record.headers().lastHeader(headerName);
        return header != null ? new String(header.value()) : null;
    }
}
