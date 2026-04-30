package com.n11.bootcamp.common_lib.event.payment;

import java.util.UUID;

public record PaymentEventPayload(
        UUID eventId,
        UUID orderId,
        UUID paymentId,
        String status,
        String correlationId,
        String failReason
) {}
