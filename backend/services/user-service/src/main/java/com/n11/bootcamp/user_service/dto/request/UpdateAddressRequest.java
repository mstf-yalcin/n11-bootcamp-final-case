package com.n11.bootcamp.user_service.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @Size(max = 50)  String title,
        @Size(max = 100) String contactName,
        @Size(max = 500) String fullAddress,
        @Size(max = 100) String city,
        @Size(max = 100) String district,
        @Size(max = 100) String country,
        @Size(max = 10)  String zipCode,
        @Pattern(regexp = "^(\\+90)?5\\d{9}$",
                message = "Phone must be a valid Turkish mobile number (5XXXXXXXXX or +905XXXXXXXXX)") String phone,
        Boolean isDefault
) {}
