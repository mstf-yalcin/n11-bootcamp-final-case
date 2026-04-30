package com.n11.bootcamp.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/user")
    public ResponseEntity<Map<String, Object>> userFallback() {
        return fallback("user-service");
    }

    @RequestMapping("/product")
    public ResponseEntity<Map<String, Object>> productFallback() {
        return fallback("product-service");
    }

    @RequestMapping("/cart")
    public ResponseEntity<Map<String, Object>> cartFallback() {
        return fallback("cart-service");
    }

    @RequestMapping("/order")
    public ResponseEntity<Map<String, Object>> orderFallback() {
        return fallback("order-service");
    }

    private ResponseEntity<Map<String, Object>> fallback(String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "message", service + " is currently unavailable. Please try again later.",
                        "errorCode", "SERVICE_UNAVAILABLE"
                ));
    }
}
