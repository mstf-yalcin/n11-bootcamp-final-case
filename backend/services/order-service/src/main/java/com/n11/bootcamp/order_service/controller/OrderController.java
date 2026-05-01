package com.n11.bootcamp.order_service.controller;

import com.n11.bootcamp.common_lib.auth.UserPrincipal;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.order_service.dto.request.CreateOrderRequest;
import com.n11.bootcamp.order_service.dto.response.OrderResponse;
import com.n11.bootcamp.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order saga producer")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create a new order from the user's cart (saga starts: ORDER_CREATED -> stock -> payment -> confirm)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);
        OrderResponse order = orderService.createOrder(
                UUID.fromString(principal.id()), principal.email(), ip, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created"));
    }

    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order by id (status polling endpoint)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        OrderResponse order = orderService.getOrder(id, UUID.fromString(principal.id()));
        return ResponseEntity.ok(ApiResponse.success(order, "Order fetched"));
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's orders, newest first")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<OrderResponse> page = orderService.listOrders(UUID.fromString(principal.id()), pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order (only allowed in PENDING or STOCK_RESERVED state)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        OrderResponse order = orderService.cancelOrder(id, UUID.fromString(principal.id()));
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled"));
    }
}
