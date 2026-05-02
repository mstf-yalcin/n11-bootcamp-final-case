package com.n11.bootcamp.user_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidAdminOperationException extends BaseException {
    public InvalidAdminOperationException(String message) {
        super(message, "INVALID_ADMIN_OPERATION", HttpStatus.BAD_REQUEST);
    }
}
