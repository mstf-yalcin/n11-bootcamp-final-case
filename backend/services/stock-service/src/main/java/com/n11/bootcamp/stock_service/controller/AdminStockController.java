package com.n11.bootcamp.stock_service.controller;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.stock_service.dto.response.ReservationResponse;
import com.n11.bootcamp.stock_service.entity.ReservationStatus;
import com.n11.bootcamp.stock_service.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reservations")
@Tag(name = "Admin Reservations", description = "Admin endpoints for stock reservations")
public class AdminStockController {

    private final StockService stockService;

    public AdminStockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    @Operation(summary = "Search stock reservations with filters (admin only)")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> searchReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID orderId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<ReservationResponse> page = stockService.searchAdminReservations(status, productId, orderId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}
