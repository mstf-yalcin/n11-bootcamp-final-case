package com.n11.bootcamp.product_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class SlugAlreadyExistsException extends BaseException {

    public SlugAlreadyExistsException(String slug) {
        super("Product with slug '" + slug + "' already exists", "SLUG_ALREADY_EXISTS", HttpStatus.CONFLICT);
    }
}
