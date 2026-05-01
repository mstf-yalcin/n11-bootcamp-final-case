package com.n11.bootcamp.payment_service.controller;

import com.n11.bootcamp.common_lib.auth.UserPrincipal;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.payment_service.dto.response.PaymentResponse;
import com.n11.bootcamp.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get payment status for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getByOrderId(orderId), "Payment fetched"));
    }

    @GetMapping("/me")
    @Operation(summary = "List the authenticated user's payments, newest first")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listMyPayments(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<PaymentResponse> page = paymentService.listByUser(UUID.fromString(principal.id()), pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}
