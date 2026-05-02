package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.product_service.client.StockClient;
import com.n11.bootcamp.product_service.client.dto.StockAvailabilityClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class StockAvailabilityCache {

    private static final String KEY_PREFIX = "stock:avail:";

    private final RedisTemplate<String, StockAvailabilityClientResponse> redisTemplate;
    private final StockClient stockClient;
    private final Duration ttl;

    public StockAvailabilityCache(RedisTemplate<String, StockAvailabilityClientResponse> redisTemplate,
                                  StockClient stockClient,
                                  @Value("${app.stock.cache-ttl-seconds:60}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.stockClient = stockClient;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Map<UUID, StockAvailabilityClientResponse> getAvailabilityMap(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, StockAvailabilityClientResponse> result = new HashMap<>();
        List<String> keys = productIds.stream().map(this::key).toList();
        List<StockAvailabilityClientResponse> cached = redisTemplate.opsForValue().multiGet(keys);

        List<UUID> missing = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            StockAvailabilityClientResponse hit = cached != null ? cached.get(i) : null;
            if (hit != null) {
                result.put(productIds.get(i), hit);
            } else {
                missing.add(productIds.get(i));
            }
        }

        if (missing.isEmpty()) {
            return result;
        }

        log.debug("Stock cache miss for {} ids out of {}", missing.size(), productIds.size());

        ApiResponse<List<StockAvailabilityClientResponse>> response = stockClient.getAvailability(missing);
        if (response == null || response.data() == null) {
            return result;
        }

        for (StockAvailabilityClientResponse item : response.data()) {
            result.put(item.productId(), item);
            redisTemplate.opsForValue().set(key(item.productId()), item, ttl);
        }

        return result;
    }

    private String key(UUID productId) {
        return KEY_PREFIX + productId;
    }
}
