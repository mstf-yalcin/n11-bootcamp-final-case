package com.n11.bootcamp.common_lib.event.payment;

import java.util.UUID;

public record PaymentEventPayload(
        UUID orderId,
        UUID paymentId,
        String status,
        String failReason
) {}
