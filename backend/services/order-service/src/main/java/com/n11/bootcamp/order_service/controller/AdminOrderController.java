package com.n11.bootcamp.order_service.controller;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.common_lib.event.order.OrderStatus;
import com.n11.bootcamp.order_service.dto.request.AdminUpdateOrderStatusRequest;
import com.n11.bootcamp.order_service.dto.response.OrderResponse;
import com.n11.bootcamp.order_service.service.OrderService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin Orders", description = "Admin endpoints for managing orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Search all orders with filters (admin only)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> searchOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<OrderResponse> page = orderService.searchAdminOrders(status, userId, from, to, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get any order by id (admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAdminOrder(id), "Order fetched"));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Manually transition an order's status (admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.adminUpdateStatus(id, request), "Order status updated"));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel any user's order (allowed in PENDING, STOCK_RESERVED, CONFIRMED — refund triggered for paid orders)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID id,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.adminCancelOrder(id, note), "Order cancelled"));
    }
}
