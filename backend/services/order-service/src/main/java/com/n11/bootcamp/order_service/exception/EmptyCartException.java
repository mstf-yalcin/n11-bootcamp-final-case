package com.n11.bootcamp.order_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class EmptyCartException extends BaseException {

    public EmptyCartException() {
        super("Cart is empty — cannot create order", "EMPTY_CART", HttpStatus.BAD_REQUEST);
    }
}
