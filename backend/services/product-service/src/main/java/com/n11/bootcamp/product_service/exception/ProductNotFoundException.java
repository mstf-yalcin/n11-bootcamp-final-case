package com.n11.bootcamp.product_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ProductNotFoundException extends BaseException {

    public ProductNotFoundException(UUID id) {
        super("Product not found with id: " + id, "PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public ProductNotFoundException(String slug) {
        super("Product not found with slug: " + slug, "PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

}
