package com.n11.bootcamp.user_service.dto.response;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        String title,
        String contactName,
        String fullAddress,
        String city,
        String district,
        String country,
        String zipCode,
        String phone,
        boolean isDefault
) {}
