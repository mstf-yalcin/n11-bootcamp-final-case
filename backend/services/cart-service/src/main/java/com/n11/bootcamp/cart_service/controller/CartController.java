package com.n11.bootcamp.cart_service.controller;

import com.n11.bootcamp.cart_service.dto.request.AddItemRequest;
import com.n11.bootcamp.cart_service.dto.request.MergeRequest;
import com.n11.bootcamp.cart_service.dto.request.UpdateItemRequest;
import com.n11.bootcamp.cart_service.dto.response.CartResponse;
import com.n11.bootcamp.cart_service.dto.response.MergeCartResponse;
import com.n11.bootcamp.cart_service.service.CartService;
import com.n11.bootcamp.common_lib.auth.UserPrincipal;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Cart management endpoints")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get current user's cart  (with product info)")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        CartResponse cart = cartService.getCart(UUID.fromString(principal.id()));
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart fetched"));
    }

    @PostMapping("/items")
    @Operation(summary = "Add or increment an item in the cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddItemRequest request) {
        CartResponse cart = cartService.addItem(UUID.fromString(principal.id()), request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item added to cart"));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update item quantity in the cart")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateItemRequest request) {
        CartResponse cart = cartService.updateItem(UUID.fromString(principal.id()), productId, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item updated"));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove an item from the cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {
        CartResponse cart = cartService.removeItem(UUID.fromString(principal.id()), productId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item removed"));
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge anonymous (localStorage) cart items into the user's cart on login. Non-existent products are skipped and returned in skippedProductIds.")
    public ResponseEntity<ApiResponse<MergeCartResponse>> mergeCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MergeRequest request) {
        MergeCartResponse result = cartService.mergeAnonymousCart(UUID.fromString(principal.id()), request);
        return ResponseEntity.ok(ApiResponse.success(result, "Cart merged"));
    }

    @DeleteMapping
    @Operation(summary = "Clear the cart (called by order-service after order confirmation)")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(UUID.fromString(principal.id()));
        return ResponseEntity.ok(ApiResponse.success("Cart cleared"));
    }
}
