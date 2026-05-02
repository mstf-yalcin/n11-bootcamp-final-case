package com.n11.bootcamp.product_service.client;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.product_service.client.dto.StockAvailabilityClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class StockClientFallbackFactory implements FallbackFactory<StockClient> {

    @Override
    public StockClient create(Throwable cause) {
        log.error("Stock service failed: {}", cause.toString(), cause);

        return new StockClient() {

            @Override
            public ApiResponse<List<StockAvailabilityClientResponse>> getAvailability(List<UUID> productIds) {
                log.warn("Fallback triggered: getAvailability productIds={}", productIds);
                return ApiResponse.success(List.of(), "stock-service unavailable");
            }
        };
    }
}
