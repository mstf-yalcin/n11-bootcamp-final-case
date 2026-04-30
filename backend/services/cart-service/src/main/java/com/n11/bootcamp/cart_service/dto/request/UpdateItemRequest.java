package com.n11.bootcamp.cart_service.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateItemRequest(
        @Min(value = 1, message = "quantity must be at least 1")
        int quantity
) {
}
