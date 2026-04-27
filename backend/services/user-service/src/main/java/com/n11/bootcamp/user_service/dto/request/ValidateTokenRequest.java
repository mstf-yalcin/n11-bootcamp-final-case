package com.n11.bootcamp.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequest(
        @NotBlank(message = "Token must not be blank")
        String token) {
}
