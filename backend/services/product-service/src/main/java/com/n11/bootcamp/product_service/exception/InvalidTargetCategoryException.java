package com.n11.bootcamp.product_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidTargetCategoryException extends BaseException {

    public InvalidTargetCategoryException(String reason) {
        super("Invalid target category: " + reason, "INVALID_TARGET_CATEGORY", HttpStatus.BAD_REQUEST);
    }
}
