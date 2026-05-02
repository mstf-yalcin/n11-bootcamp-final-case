package com.n11.bootcamp.payment_service.dto.request;

import jakarta.validation.constraints.Size;

public record AdminRefundRequest(
        @Size(max = 480, message = "reason must be at most 480 characters")
        String reason
) {}
