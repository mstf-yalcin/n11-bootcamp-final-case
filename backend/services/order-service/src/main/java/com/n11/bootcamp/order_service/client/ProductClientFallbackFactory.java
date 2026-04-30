package com.n11.bootcamp.order_service.client;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.order_service.client.dto.ProductClientResponse;
import com.n11.bootcamp.order_service.exception.ProductServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        log.error("Product service unavailable: {}", cause.toString());

        return new ProductClient() {
            @Override
            public ApiResponse<List<ProductClientResponse>> getProductsByIds(List<UUID> ids) {
                log.warn("Fallback triggered: getProductsByIds ids={}", ids);
                throw new ProductServiceUnavailableException(cause);
            }
        };
    }
}
