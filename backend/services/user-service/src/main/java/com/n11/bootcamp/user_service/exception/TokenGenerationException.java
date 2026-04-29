package com.n11.bootcamp.user_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class TokenGenerationException extends BaseException {

    public TokenGenerationException(Throwable cause) {
        super("Token generation failed", "TOKEN_GENERATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
