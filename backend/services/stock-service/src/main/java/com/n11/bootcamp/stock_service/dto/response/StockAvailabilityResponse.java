package com.n11.bootcamp.stock_service.dto.response;

import com.n11.bootcamp.stock_service.entity.StockStatus;

import java.util.UUID;

public record StockAvailabilityResponse(
        UUID productId,
        int available,
        StockStatus status
) {}
