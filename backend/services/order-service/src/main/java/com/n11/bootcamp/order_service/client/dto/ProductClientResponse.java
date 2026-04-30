package com.n11.bootcamp.order_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductClientResponse(
        UUID id,
        String name,
        BigDecimal price,
        String currency,
        String imageUrl
) {}
