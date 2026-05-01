package com.n11.bootcamp.user_service.exception;

import com.n11.bootcamp.common_lib.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AddressNotFoundException extends BaseException {

    public AddressNotFoundException(UUID addressId) {
        super("Address not found: " + addressId, "ADDRESS_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
