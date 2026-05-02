package com.n11.bootcamp.user_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class PhoneAlreadyExistsException extends BaseException {
    public PhoneAlreadyExistsException() {
        super("This phone number cannot be used", "PHONE_ALREADY_EXISTS", HttpStatus.CONFLICT);
    }
}
