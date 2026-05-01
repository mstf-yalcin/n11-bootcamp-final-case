package com.n11.bootcamp.payment_service.gateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PaymentGatewayRequest(
        UUID orderId,
        UUID userId,
        String userEmail,
        BigDecimal amount,
        String currency,
        List<Item> items,
        String correlationId
) {
    public record Item(UUID productId, Integer quantity, BigDecimal unitPrice) {}
}
