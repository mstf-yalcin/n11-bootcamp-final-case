package com.n11.bootcamp.cart_service.client;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.cart_service.client.dto.StockAvailabilityClientResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Retry(name = "stock-service")
@FeignClient(name = "stock-service", fallbackFactory = StockClientFallbackFactory.class)
public interface StockClient {

    @GetMapping("/api/v1/stocks/availability")
    ApiResponse<List<StockAvailabilityClientResponse>> getAvailability(@RequestParam("productIds") List<UUID> productIds);
}
