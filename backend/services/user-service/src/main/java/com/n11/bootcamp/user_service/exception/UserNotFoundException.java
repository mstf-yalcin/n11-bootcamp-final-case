package com.n11.bootcamp.user_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException(String email) {
        super("User not found: " + email, "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
