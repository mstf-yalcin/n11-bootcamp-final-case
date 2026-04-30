package com.n11.bootcamp.cart_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID userId,
        List<CartItemResponse> items,
        BigDecimal totalAmount,
        String currency,
        Instant updatedAt
) {

    public static CartResponse empty(UUID userId) {
        return new CartResponse(userId, List.of(), BigDecimal.ZERO, "TRY", Instant.now());
    }
}
