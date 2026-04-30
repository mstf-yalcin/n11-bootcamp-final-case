package com.n11.bootcamp.order_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrderNotFoundException extends BaseException {

    public OrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId, "ORDER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
