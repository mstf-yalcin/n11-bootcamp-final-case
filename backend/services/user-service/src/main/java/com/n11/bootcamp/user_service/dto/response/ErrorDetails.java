package com.n11.bootcamp.user_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetails(String message, String field, String value, String code) {

    public ErrorDetails(String message) {
        this(message, null, null, null);
    }

    public ErrorDetails(String message, String field, String value) {
        this(message, field, value, null);
    }
}
