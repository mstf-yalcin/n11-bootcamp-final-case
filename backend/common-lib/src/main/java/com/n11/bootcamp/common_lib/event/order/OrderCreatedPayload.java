package com.n11.bootcamp.common_lib.event.order;

import java.util.List;
import java.util.UUID;

public record OrderCreatedPayload(
        UUID eventId,
        UUID orderId,
        UUID userId,
        String correlationId,
        List<OrderEventItem> items
) {}
