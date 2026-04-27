package com.n11.bootcamp.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @Email
        @NotBlank(message = "Email is required") String email,
        @NotBlank(message = "Password is required") String password) {
}
