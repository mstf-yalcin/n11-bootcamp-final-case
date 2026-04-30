package com.n11.bootcamp.common_lib.event.order;

import java.util.UUID;

public record OrderConfirmedPayload(
        UUID orderId,
        UUID userId
) {}
