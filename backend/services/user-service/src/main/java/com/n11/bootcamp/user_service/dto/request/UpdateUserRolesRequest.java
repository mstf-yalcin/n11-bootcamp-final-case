package com.n11.bootcamp.user_service.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateUserRolesRequest(
        @NotEmpty(message = "roles must not be empty")
        List<String> roles
) {}
