package com.n11.bootcamp.order_service.client.dto;

public record CheckoutContextClientResponse(
        UserInfoClientResponse user,
        AddressClientResponse address
) {}
