package com.n11.bootcamp.order_service.client;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.order_service.client.dto.CartClientResponse;
import com.n11.bootcamp.order_service.exception.CartServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CartClientFallbackFactory implements FallbackFactory<CartClient> {

    @Override
    public CartClient create(Throwable cause) {
        log.error("Cart service unavailable: {}", cause.toString());

        return new CartClient() {
            @Override
            public ApiResponse<CartClientResponse> getCart() {
                log.warn("Fallback triggered: getCart");
                throw new CartServiceUnavailableException(cause);
            }

            @Override
            public ApiResponse<Void> clearCart() {
                log.warn("Fallback triggered: clearCart — non-fatal, cart will expire via TTL");
                return ApiResponse.success("cart-service unavailable, fallback noop");
            }
        };
    }
}
