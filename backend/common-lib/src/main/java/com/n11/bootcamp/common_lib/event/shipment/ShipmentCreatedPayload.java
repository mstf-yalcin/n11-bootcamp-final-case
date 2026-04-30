package com.n11.bootcamp.common_lib.event.shipment;

import java.util.UUID;

public record ShipmentCreatedPayload(
        UUID orderId,
        UUID shipmentId,
        String trackingNumber
) {}
