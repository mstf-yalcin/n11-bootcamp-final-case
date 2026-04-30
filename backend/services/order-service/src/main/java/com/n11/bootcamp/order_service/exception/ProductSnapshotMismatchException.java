package com.n11.bootcamp.order_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

public class ProductSnapshotMismatchException extends BaseException {

    public ProductSnapshotMismatchException(List<UUID> missingIds) {
        super(
                "Some cart products are no longer available: " + missingIds,
                "PRODUCT_SNAPSHOT_MISMATCH",
                HttpStatus.CONFLICT
        );
    }
}
