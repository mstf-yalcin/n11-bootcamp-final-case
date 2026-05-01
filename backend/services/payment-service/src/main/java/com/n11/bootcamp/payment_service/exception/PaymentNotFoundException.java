package com.n11.bootcamp.payment_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PaymentNotFoundException extends BaseException {

    public PaymentNotFoundException(UUID orderId) {
        super("Payment not found for orderId: " + orderId, "PAYMENT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
