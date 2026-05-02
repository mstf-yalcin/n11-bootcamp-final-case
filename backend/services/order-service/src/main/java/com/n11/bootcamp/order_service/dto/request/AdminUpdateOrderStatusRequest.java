package com.n11.bootcamp.order_service.dto.request;

import com.n11.bootcamp.common_lib.event.order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUpdateOrderStatusRequest(
        @NotNull(message = "status is required")
        OrderStatus status,

        @Size(max = 500, message = "note must be at most 500 characters")
        String note
) {}
