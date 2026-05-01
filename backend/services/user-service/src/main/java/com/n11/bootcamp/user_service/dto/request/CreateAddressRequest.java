package com.n11.bootcamp.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 50)
        String title,

        @NotBlank(message = "Contact name is required")
        @Size(max = 100)
        String contactName,

        @NotBlank(message = "Full address is required")
        @Size(max = 500)
        String fullAddress,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @Size(max = 100)
        String district,

        @Size(max = 100)
        String country,

        @Size(max = 10)
        String zipCode,

        @Pattern(regexp = "^(\\+90)?5\\d{9}$",
                message = "Phone must be a valid Turkish mobile number (5XXXXXXXXX or +905XXXXXXXXX)")
        String phone,

        Boolean isDefault
) {}
