package com.n11.bootcamp.cart_service.client;

import com.n11.bootcamp.cart_service.client.dto.ProductClientResponse;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
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
        log.error("Product service failed: {}", cause.toString(), cause);

        return new ProductClient() {

            @Override
            public ApiResponse<List<ProductClientResponse>> getProductsByIds(List<UUID> ids) {
                log.warn("Fallback triggered: getProductsByIds ids={}", ids);
                return ApiResponse.success(List.of(), "product-service unavailable");
            }

            @Override
            public ApiResponse<List<UUID>> getExistingProductIds(List<UUID> ids) {
                log.warn("Fallback triggered: getExistingProductIds ids={}", ids);
                return ApiResponse.success(List.of(), "product-service unavailable");
            }
        };
    }
}
