package com.n11.bootcamp.payment_service.gateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PaymentGatewayRequest(
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        List<Item> items,
        Buyer buyer,
        Address address,
        String correlationId
) {
    public record Item(UUID productId, Integer quantity, BigDecimal unitPrice) {}

    public record Buyer(
            String firstName,
            String lastName,
            String email,
            String phone,
            String identityNumber,
            String ip
    ) {}

    public record Address(
            String contactName,
            String fullAddress,
            String city,
            String district,
            String country,
            String zipCode,
            String phone
    ) {}
}
