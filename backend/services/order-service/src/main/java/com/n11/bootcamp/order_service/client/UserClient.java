package com.n11.bootcamp.order_service.client;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.order_service.client.dto.CheckoutContextClientResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Retry(name = "user-service")
@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/api/v1/users/me/checkout-context")
    ApiResponse<CheckoutContextClientResponse> getCheckoutContext(@RequestParam("addressId") UUID addressId);
}
