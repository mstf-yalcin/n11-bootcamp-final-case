package com.n11.bootcamp.common_lib.event.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderEventItem(
        UUID productId,
        Integer quantity,
        BigDecimal unitPrice
) {}
