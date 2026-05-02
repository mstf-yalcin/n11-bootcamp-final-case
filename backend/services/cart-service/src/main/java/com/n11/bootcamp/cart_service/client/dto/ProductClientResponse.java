package com.n11.bootcamp.cart_service.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductClientResponse(
        UUID id,
        String name,
        BigDecimal price,
        String currency,
        String imageUrl,
        String stockStatus,
        Integer availableQuantity
) {
}
