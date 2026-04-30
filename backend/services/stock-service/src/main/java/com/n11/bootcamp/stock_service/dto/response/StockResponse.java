package com.n11.bootcamp.stock_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StockResponse(
        UUID id,
        UUID productId,
        int quantity,
        int reserved,
        int available,
        Instant updatedAt
) {}
