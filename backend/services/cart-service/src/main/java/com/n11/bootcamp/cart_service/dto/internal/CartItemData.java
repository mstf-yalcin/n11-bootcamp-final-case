package com.n11.bootcamp.cart_service.dto.internal;

import java.util.UUID;

public record CartItemData(UUID productId, int quantity) {
}
