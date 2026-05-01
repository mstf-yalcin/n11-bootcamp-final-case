package com.n11.bootcamp.api_gateway.security;

import com.n11.bootcamp.api_gateway.exception.ErrorResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ErrorResponseBuilder errorResponseBuilder;

    public CustomAuthenticationEntryPoint(ErrorResponseBuilder errorResponseBuilder) {
        this.errorResponseBuilder = errorResponseBuilder;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authenticationException) {
        return errorResponseBuilder.buildErrorResponse(
                exchange,
                HttpStatus.UNAUTHORIZED,
                authenticationException.getMessage(),
                "INVALID_TOKEN");
    }
}
