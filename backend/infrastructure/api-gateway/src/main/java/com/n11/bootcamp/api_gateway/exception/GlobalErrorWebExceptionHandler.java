package com.n11.bootcamp.api_gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Order(-2)
@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode status = resolveStatus(ex);
        String code = resolveCode(status);
        String message = resolveMessage(ex, status, exchange);

        log.warn("Gateway error [{}] {} : {}",
                status.value(), exchange.getRequest().getPath(), message);

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(ApiResponse.fail(message, code));
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    private HttpStatusCode resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return rse.getStatusCode();
        }
        if (ex instanceof TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (ex instanceof ConnectException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveCode(HttpStatusCode status) {
        if (status instanceof HttpStatus hs) {
            return hs.name();
        }
        return "GATEWAY_ERROR";
    }

    private String resolveMessage(Throwable ex, HttpStatusCode status, ServerWebExchange exchange) {
        if (status.value() == 404) {
            return "Endpoint not found: " + exchange.getRequest().getPath().value();
        }
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
            return rse.getReason();
        }
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }
        return status instanceof HttpStatus hs ? hs.getReasonPhrase() : "Unexpected error";
    }
}
