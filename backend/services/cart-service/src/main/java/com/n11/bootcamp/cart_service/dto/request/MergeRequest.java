package com.n11.bootcamp.cart_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MergeRequest(
        @NotNull(message = "items is required")
        @Valid
        List<AddItemRequest> items
) {
}
