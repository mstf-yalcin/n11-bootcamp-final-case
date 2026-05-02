package com.n11.bootcamp.payment_service.controller;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.payment_service.dto.request.AdminRefundRequest;
import com.n11.bootcamp.payment_service.dto.response.PaymentResponse;
import com.n11.bootcamp.payment_service.entity.PaymentStatus;
import com.n11.bootcamp.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments")
@Tag(name = "Admin Payments", description = "Admin endpoints for payments and refunds")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "Search all payments with filters (admin only)")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> searchPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<PaymentResponse> page = paymentService.searchAdminPayments(status, userId, from, to, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PostMapping("/{orderId}/refund")
    @Operation(summary = "Manually refund a completed payment (admin only)")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) AdminRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.adminRefund(orderId, request), "Refund initiated"));
    }
}
