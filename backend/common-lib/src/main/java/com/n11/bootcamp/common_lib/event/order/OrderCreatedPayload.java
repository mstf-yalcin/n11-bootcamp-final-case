package com.n11.bootcamp.common_lib.event.order;

import java.util.List;
import java.util.UUID;

public record OrderCreatedPayload(
        UUID orderId,
        UUID userId,
        List<OrderEventItem> items,
        BuyerInfo buyer,
        ShippingAddress shippingAddress
) {}
