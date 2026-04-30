package com.n11.bootcamp.cart_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ProductNotFoundException extends BaseException {

    public ProductNotFoundException(UUID productId) {
        super("Product not found: " + productId, "PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
