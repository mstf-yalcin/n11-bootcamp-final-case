package com.n11.bootcamp.order_service.client.dto;

import java.util.UUID;

public record AddressClientResponse(
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
