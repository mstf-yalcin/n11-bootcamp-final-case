package com.n11.bootcamp.common_lib.event.stock;

import java.util.UUID;

public record StockReservedPayload(
        UUID orderId
) {}
