package com.n11.bootcamp.common_lib.event.order;

public record BuyerInfo(
        String firstName,
        String lastName,
        String email,
        String phone,
        String identityNumber,  
        String ip             
) {}
