package com.n11.bootcamp.product_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.product_service.client.dto.StockAvailabilityClientResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, StockAvailabilityClientResponse> stockAvailabilityRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();

        Jackson2JsonRedisSerializer<StockAvailabilityClientResponse> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, StockAvailabilityClientResponse.class);

        RedisTemplate<String, StockAvailabilityClientResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
