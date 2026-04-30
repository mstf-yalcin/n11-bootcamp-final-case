package com.n11.bootcamp.order_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ProductServiceUnavailableException extends BaseException {

    public ProductServiceUnavailableException(Throwable cause) {
        super(
                "Product service is currently unavailable, please try again",
                "PRODUCT_SERVICE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE,
                cause
        );
    }
}
