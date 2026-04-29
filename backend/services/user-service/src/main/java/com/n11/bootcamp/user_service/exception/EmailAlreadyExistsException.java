package com.n11.bootcamp.user_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BaseException {
    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email, "EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT);
    }
}
