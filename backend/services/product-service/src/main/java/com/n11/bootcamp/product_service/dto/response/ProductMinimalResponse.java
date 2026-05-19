package com.n11.bootcamp.product_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductMinimalResponse(
        UUID id,
        String slug,
        String name,
        BigDecimal price,
        String currency,
        String imageUrl,
        String stockStatus,
        Integer availableQuantity
) {
    public ProductMinimalResponse withStock(String stockStatus, Integer availableQuantity) {
        return new ProductMinimalResponse(id, slug, name, price, currency, imageUrl, stockStatus, availableQuantity);
    }
}
