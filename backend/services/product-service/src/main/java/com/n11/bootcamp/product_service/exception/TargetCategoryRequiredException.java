package com.n11.bootcamp.product_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

public class TargetCategoryRequiredException extends BaseException {

    public TargetCategoryRequiredException(long productCount) {
        super(
                "Category has " + productCount + " active product(s); a target category must be provided to move them before deletion",
                "TARGET_CATEGORY_REQUIRED",
                HttpStatus.CONFLICT
        );
    }
}
