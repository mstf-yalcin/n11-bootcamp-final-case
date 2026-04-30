package com.n11.bootcamp.common_lib.event.order;

import java.util.UUID;

public record OrderCancelledPayload(
        UUID orderId,
        UUID userId,
        String cancelReason
) {}
