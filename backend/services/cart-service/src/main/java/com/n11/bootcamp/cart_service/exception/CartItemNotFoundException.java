package com.n11.bootcamp.cart_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CartItemNotFoundException extends BaseException {

    public CartItemNotFoundException(UUID productId) {
        super("Cart item not found for productId: " + productId, "CART_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
