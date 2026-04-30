package com.n11.bootcamp.order_service.exception;

import com.n11.bootcamp.common_lib.event.order.OrderStatus;
import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidOrderStateException extends BaseException {

    public InvalidOrderStateException(OrderStatus current, String operation) {
        super(
                String.format("Operation '%s' not allowed in state %s", operation, current),
                "INVALID_ORDER_STATE",
                HttpStatus.CONFLICT
        );
    }
}
