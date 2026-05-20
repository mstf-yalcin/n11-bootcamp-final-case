package com.n11.bootcamp.common_lib.idempotency;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends BaseException {

    public IdempotencyConflictException() {
        super(
                "An identical request is still being processed, please retry in a moment",
                "IDEMPOTENCY_IN_PROGRESS",
                HttpStatus.CONFLICT
        );
    }
}
