package com.n11.bootcamp.payment_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import com.n11.bootcamp.payment_service.entity.PaymentStatus;
import org.springframework.http.HttpStatus;

public class InvalidRefundStateException extends BaseException {
    public InvalidRefundStateException(PaymentStatus status) {
        super("Refund not allowed for payment in state: " + status,
                "INVALID_REFUND_STATE", HttpStatus.CONFLICT);
    }

    public InvalidRefundStateException(String message) {
        super(message, "REFUND_FAILED", HttpStatus.BAD_GATEWAY);
    }
}
