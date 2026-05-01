package com.n11.bootcamp.order_service.client;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.order_service.client.dto.CheckoutContextClientResponse;
import com.n11.bootcamp.order_service.exception.UserServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        log.error("User service unavailable: {}", cause.toString());

        return new UserClient() {
            @Override
            public ApiResponse<CheckoutContextClientResponse> getCheckoutContext(UUID addressId) {
                log.warn("Fallback triggered: getCheckoutContext addressId={}", addressId);
                throw new UserServiceUnavailableException(cause);
            }
        };
    }
}
