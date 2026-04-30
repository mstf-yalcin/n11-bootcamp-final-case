package com.n11.bootcamp.order_service.client;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.order_service.client.dto.ProductClientResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Retry(name = "product-service")
@FeignClient(name = "product-service", fallbackFactory = ProductClientFallbackFactory.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/batch")
    ApiResponse<List<ProductClientResponse>> getProductsByIds(@RequestParam List<UUID> ids);
}
