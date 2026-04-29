package com.n11.bootcamp.product_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String slug,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Integer ratingCount,
        BigDecimal ratingAverage,
        String imageUrl,
        Set<TagResponse> tags,
        UUID categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt
) {
}
