package com.n11.bootcamp.product_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CategoryNotFoundException extends BaseException {

    public CategoryNotFoundException(UUID id) {
        super("Category not found with id: " + id, "CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
