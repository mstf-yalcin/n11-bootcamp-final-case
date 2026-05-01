package com.n11.bootcamp.order_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserServiceUnavailableException extends BaseException {

    public UserServiceUnavailableException() {
        super("User service unavailable", "USER_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
    }

    public UserServiceUnavailableException(Throwable cause) {
        this();
        initCause(cause);
    }
}
