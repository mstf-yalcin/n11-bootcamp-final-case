package com.n11.bootcamp.stock_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateStockRequest(
        @NotNull(message = "quantity is required")
        @Min(value = 0, message = "quantity must be >= 0")
        Integer quantity
) {}
