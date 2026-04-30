package com.n11.bootcamp.order_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        String currency,
        BigDecimal subtotal
) {}
