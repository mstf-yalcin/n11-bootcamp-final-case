package com.n11.bootcamp.order_service.client;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.order_service.client.dto.CartClientResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

@Retry(name = "cart-service")
@FeignClient(name = "cart-service", fallbackFactory = CartClientFallbackFactory.class)
public interface CartClient {

    @GetMapping("/api/v1/cart")
    ApiResponse<CartClientResponse> getCart();

    @DeleteMapping("/api/v1/cart")
    ApiResponse<Void> clearCart();
}
