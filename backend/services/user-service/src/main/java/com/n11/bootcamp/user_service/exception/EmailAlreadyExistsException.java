package com.n11.bootcamp.user_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BaseException {
    public EmailAlreadyExistsException() {
        super("This email cannot be used", "EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT);
    }
}
