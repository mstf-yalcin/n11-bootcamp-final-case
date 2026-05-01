package com.n11.bootcamp.api_gateway.controller;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/user")
    public ResponseEntity<ApiResponse<Void>> userFallback() {
        return fallback("user-service");
    }

    @RequestMapping("/product")
    public ResponseEntity<ApiResponse<Void>> productFallback() {
        return fallback("product-service");
    }

    @RequestMapping("/cart")
    public ResponseEntity<ApiResponse<Void>> cartFallback() {
        return fallback("cart-service");
    }

    @RequestMapping("/order")
    public ResponseEntity<ApiResponse<Void>> orderFallback() {
        return fallback("order-service");
    }

    @RequestMapping("/payment")
    public ResponseEntity<ApiResponse<Void>> paymentFallback() {
        return fallback("payment-service");
    }

    @RequestMapping("/stock")
    public ResponseEntity<ApiResponse<Void>> stockFallback() {
        return fallback("stock-service");
    }

    private ResponseEntity<ApiResponse<Void>> fallback(String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(
                        service + " is currently unavailable. Please try again later.",
                        "SERVICE_UNAVAILABLE"));
    }
}
