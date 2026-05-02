package com.n11.bootcamp.cart_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class StockServiceUnavailableException extends BaseException {

    public StockServiceUnavailableException() {
        super("Stock service is currently unavailable", "STOCK_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
