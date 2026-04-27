package com.n11.bootcamp.user_service.exception;

public class TokenGenerationException extends RuntimeException {

    public TokenGenerationException(Throwable cause) {
        super("Token generation failed", cause);
    }
}
