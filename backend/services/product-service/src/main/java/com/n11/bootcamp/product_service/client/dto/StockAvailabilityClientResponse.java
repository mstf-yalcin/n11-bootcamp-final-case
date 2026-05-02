package com.n11.bootcamp.product_service.client.dto;

import java.util.UUID;

public record StockAvailabilityClientResponse(
        UUID productId,
        int available,
        String status
) {}
