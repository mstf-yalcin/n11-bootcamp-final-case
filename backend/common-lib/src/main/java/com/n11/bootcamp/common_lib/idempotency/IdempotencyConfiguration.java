package com.n11.bootcamp.common_lib.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "app.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotencyConfiguration {

    @Bean
    public IdempotencyAspect idempotencyAspect(StringRedisTemplate redis,
                                               ObjectMapper objectMapper,
                                               @Value("${spring.application.name:unknown}") String serviceName) {
        return new IdempotencyAspect(redis, objectMapper, serviceName);
    }
}
