package com.n11.bootcamp.user_service.dto.response;

import java.util.UUID;

public record UserInfoResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone
) {}
