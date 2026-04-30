package com.n11.bootcamp.order_service.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "addressId is required")
        UUID addressId
) {}
