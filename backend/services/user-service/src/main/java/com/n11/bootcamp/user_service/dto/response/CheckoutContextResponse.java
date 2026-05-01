package com.n11.bootcamp.user_service.dto.response;

public record CheckoutContextResponse(
        UserInfoResponse user,
        AddressResponse address
) {}
