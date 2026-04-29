package com.n11.bootcamp.user_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseException {
    public InvalidTokenException(String message) {
        super(message, "INVALID_TOKEN", HttpStatus.UNAUTHORIZED);
    }
}
