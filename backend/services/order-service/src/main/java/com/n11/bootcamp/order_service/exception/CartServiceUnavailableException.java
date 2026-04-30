package com.n11.bootcamp.order_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class CartServiceUnavailableException extends BaseException {

    public CartServiceUnavailableException(Throwable cause) {
        super(
                "Cart service is currently unavailable, please try again",
                "CART_SERVICE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE,
                cause
        );
    }
}
