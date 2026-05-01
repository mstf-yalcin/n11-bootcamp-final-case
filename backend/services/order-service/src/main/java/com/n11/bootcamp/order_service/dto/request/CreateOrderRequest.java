package com.n11.bootcamp.order_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "addressId is required")
        UUID addressId,

        @NotNull(message = "identityNumber (TC) is required")
        @Pattern(regexp = "\\d{11}", message = "identityNumber must be 11 digits")
        String identityNumber
) {}
