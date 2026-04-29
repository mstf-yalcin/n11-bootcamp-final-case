package com.n11.bootcamp.product_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TagNotFoundException extends BaseException {

    public TagNotFoundException(UUID id) {
        super("Tag not found with id: " + id, "TAG_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
