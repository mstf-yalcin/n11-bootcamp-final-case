package com.n11.bootcamp.cart_service.dto.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CartData(UUID userId, List<CartItemData> items, Instant updatedAt) {

    public static CartData empty(UUID userId) {
        return new CartData(userId, new ArrayList<>(), Instant.now());
    }
}
