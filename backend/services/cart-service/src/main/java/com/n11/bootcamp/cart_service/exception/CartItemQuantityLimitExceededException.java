package com.n11.bootcamp.cart_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CartItemQuantityLimitExceededException extends BaseException {

    public CartItemQuantityLimitExceededException(UUID productId, int requested, int limit) {
        super(
                String.format("Cart item quantity limit exceeded for productId: %s — requested: %d, limit: %d",
                        productId, requested, limit),
                "CART_ITEM_QUANTITY_LIMIT_EXCEEDED",
                HttpStatus.CONFLICT
        );
    }
}
