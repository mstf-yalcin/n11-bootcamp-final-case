package com.n11.bootcamp.common_lib.exception;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.common_lib.dto.response.ErrorDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        log.warn("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.fail(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ErrorDetails> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ErrorDetails(
                        fe.getDefaultMessage(),
                        fe.getField(),
                        fe.getRejectedValue() == null ? null : String.valueOf(fe.getRejectedValue())
                ))
                .toList();
        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("Validation failed", "VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Internal server error", "INTERNAL_ERROR"));
    }
}
