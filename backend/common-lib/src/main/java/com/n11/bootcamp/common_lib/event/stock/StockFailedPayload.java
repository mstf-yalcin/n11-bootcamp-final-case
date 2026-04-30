package com.n11.bootcamp.common_lib.event.stock;

import java.util.UUID;

public record StockFailedPayload(
        UUID orderId,
        UUID failedProductId,
        Integer requested,
        Integer available
) {}
