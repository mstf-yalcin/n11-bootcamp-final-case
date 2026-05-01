package com.n11.bootcamp.common_lib.event.order;

public record ShippingAddress(
        String contactName,
        String fullAddress,
        String city,
        String district,
        String country,
        String zipCode,
        String phone
) {}
