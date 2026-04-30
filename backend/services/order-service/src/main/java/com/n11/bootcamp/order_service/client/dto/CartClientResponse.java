package com.n11.bootcamp.order_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartClientResponse(
        UUID userId,
        List<CartItemClientResponse> items,
        BigDecimal totalAmount,
        String currency,
        Instant updatedAt
) {}
