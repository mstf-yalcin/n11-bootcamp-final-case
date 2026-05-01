package com.n11.bootcamp.product_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class SlugGenerationException extends BaseException {

    public SlugGenerationException(String name) {
        super("Could not generate unique slug after retries for: " + name,
                "SLUG_GENERATION_FAILED",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
