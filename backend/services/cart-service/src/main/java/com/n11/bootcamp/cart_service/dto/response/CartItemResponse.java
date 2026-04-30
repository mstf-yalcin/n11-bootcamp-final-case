package com.n11.bootcamp.cart_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID productId,
        String productName,
        String imageUrl,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        BigDecimal subtotal
) {
}
