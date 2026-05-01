package com.n11.bootcamp.order_service.client.dto;

import java.util.UUID;

public record UserInfoClientResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone
) {}
