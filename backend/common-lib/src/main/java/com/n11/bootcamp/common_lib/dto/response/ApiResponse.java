package com.n11.bootcamp.common_lib.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String errorCode,
        PageableInfo page,
        List<ErrorDetails> errors
) {
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, null, message, null, null, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null, null, null);
    }

    public static <T> ApiResponse<List<T>> success(Page<T> page) {
        return new ApiResponse<>(true, page.getContent(), null, null, PageableInfo.from(page), null);
    }

    public static <T> ApiResponse<List<T>> success(Page<T> page, String message) {
        return new ApiResponse<>(true, page.getContent(), message, null, PageableInfo.from(page), null);
    }

    public static <T> ApiResponse<T> fail(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode, null, null);
    }

    public static <T> ApiResponse<T> fail(String message, String errorCode, List<ErrorDetails> errors) {
        return new ApiResponse<>(false, null, message, errorCode, null, errors);
    }
}
