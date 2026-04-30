package com.n11.bootcamp.stock_service.dto.response;

import com.n11.bootcamp.stock_service.entity.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID orderId,
        UUID productId,
        int quantity,
        ReservationStatus status,
        Instant createdAt
) {}
