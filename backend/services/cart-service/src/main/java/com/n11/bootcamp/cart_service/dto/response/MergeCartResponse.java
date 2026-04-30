package com.n11.bootcamp.cart_service.dto.response;

import java.util.List;
import java.util.UUID;

public record MergeCartResponse(CartResponse cart, List<UUID> skippedProductIds) {
}
