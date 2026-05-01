package com.n11.bootcamp.api_gateway.security;

import com.n11.bootcamp.api_gateway.exception.ErrorResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CustomAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    public CustomAccessDeniedHandler(ErrorResponseBuilder errorResponseBuilder) {
        this.errorResponseBuilder = errorResponseBuilder;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException accessDeniedException) {
        return errorResponseBuilder.buildErrorResponse(
                exchange,
                HttpStatus.FORBIDDEN,
                accessDeniedException.getMessage(),
                "ACCESS_DENIED");
    }
}
