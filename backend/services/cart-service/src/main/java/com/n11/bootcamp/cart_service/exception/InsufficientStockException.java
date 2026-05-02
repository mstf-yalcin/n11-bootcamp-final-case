package com.n11.bootcamp.cart_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class InsufficientStockException extends BaseException {

    public InsufficientStockException(UUID productId, int requested, int available) {
        super(
                String.format("Insufficient stock for productId: %s — requested: %d, available: %d",
                        productId, requested, available),
                "INSUFFICIENT_STOCK",
                HttpStatus.CONFLICT
        );
    }
}
