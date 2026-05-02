package com.n11.bootcamp.user_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        List<String> roles,
        boolean isActive,
        Instant createdAt
) {}
