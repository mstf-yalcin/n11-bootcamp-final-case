package com.n11.bootcamp.user_service.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "isActive is required")
        Boolean isActive
) {}
