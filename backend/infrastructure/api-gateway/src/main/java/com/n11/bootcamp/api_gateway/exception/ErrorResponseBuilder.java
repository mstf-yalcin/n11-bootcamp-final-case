package com.n11.bootcamp.api_gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ErrorResponseBuilder {

    private final ObjectMapper objectMapper;

    public ErrorResponseBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> buildErrorResponse(ServerWebExchange exchange, HttpStatus status, String message, String errorCode) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Void> body = ApiResponse.fail(message, errorCode);

        try {
            byte[] responseBytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
