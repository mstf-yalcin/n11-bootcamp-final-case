package com.n11.bootcamp.order_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartItemClientResponse(
        UUID productId,
        String productName,
        String imageUrl,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        BigDecimal subtotal
) {}
