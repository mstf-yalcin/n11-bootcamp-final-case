package com.n11.bootcamp.user_service.controller;

import com.n11.bootcamp.common_lib.auth.UserPrincipal;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.user_service.dto.request.CreateAddressRequest;
import com.n11.bootcamp.user_service.dto.request.UpdateAddressRequest;
import com.n11.bootcamp.user_service.dto.response.AddressResponse;
import com.n11.bootcamp.user_service.dto.response.CheckoutContextResponse;
import com.n11.bootcamp.user_service.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AddressService addressService;

    public UserController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> listMyAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = UUID.fromString(principal.id());
        return ResponseEntity.ok(ApiResponse.success(
                addressService.listForUser(userId), "Addresses fetched"));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAddressRequest request) {
        UUID userId = UUID.fromString(principal.id());
        AddressResponse created = addressService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Address created"));
    }

    @PutMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        UUID userId = UUID.fromString(principal.id());
        return ResponseEntity.ok(ApiResponse.success(
                addressService.update(userId, addressId, request), "Address updated"));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId) {
        UUID userId = UUID.fromString(principal.id());
        addressService.delete(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted"));
    }

    @GetMapping("/me/checkout-context")
    public ResponseEntity<ApiResponse<CheckoutContextResponse>> getCheckoutContext(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID addressId) {
        UUID userId = UUID.fromString(principal.id());
        return ResponseEntity.ok(ApiResponse.success(
                addressService.getCheckoutContext(userId, addressId),
                "Checkout context fetched"));
    }
}
