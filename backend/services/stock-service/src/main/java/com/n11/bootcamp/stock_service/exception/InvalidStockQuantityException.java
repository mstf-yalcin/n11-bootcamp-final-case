package com.n11.bootcamp.stock_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class InvalidStockQuantityException extends BaseException {

    public InvalidStockQuantityException(UUID productId, int requested, int reserved) {
        super("New quantity (%d) cannot be less than reserved amount (%d) for productId: %s"
                .formatted(requested, reserved, productId),
                "INVALID_STOCK_QUANTITY",
                HttpStatus.BAD_REQUEST);
    }
}
