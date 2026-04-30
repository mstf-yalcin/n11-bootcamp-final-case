package com.n11.bootcamp.stock_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class StockAlreadyExistsException extends BaseException {

    public StockAlreadyExistsException(UUID productId) {
        super("Stock entry already exists for productId: " + productId, "STOCK_ALREADY_EXISTS", HttpStatus.CONFLICT);
    }
}
