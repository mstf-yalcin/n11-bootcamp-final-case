package com.n11.bootcamp.product_service.dto.response;

import java.util.UUID;

public record TagResponse(
        UUID id,
        String name,
        String slug
) {
}
