package com.n11.bootcamp.stock_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class StockNotFoundException extends BaseException {

    public StockNotFoundException(UUID productId) {
        super("Stock not found for productId: " + productId, "STOCK_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
