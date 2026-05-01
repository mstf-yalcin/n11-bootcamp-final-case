package com.n11.bootcamp.payment_service.dto.response;

import com.n11.bootcamp.payment_service.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String provider,
        String providerPaymentId,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {}
