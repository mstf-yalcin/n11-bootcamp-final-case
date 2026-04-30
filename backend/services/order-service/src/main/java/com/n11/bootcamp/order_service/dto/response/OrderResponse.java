package com.n11.bootcamp.order_service.dto.response;

import com.n11.bootcamp.common_lib.event.order.CancelReason;
import com.n11.bootcamp.common_lib.event.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        CancelReason cancelReason,
        BigDecimal totalAmount,
        String currency,
        UUID addressId,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {}
