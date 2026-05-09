package com.n11.bootcamp.stock_service.controller;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.stock_service.dto.request.CreateStockRequest;
import com.n11.bootcamp.stock_service.dto.request.UpdateStockRequest;
import com.n11.bootcamp.stock_service.dto.response.ReservationResponse;
import com.n11.bootcamp.stock_service.dto.response.StockAvailabilityResponse;
import com.n11.bootcamp.stock_service.dto.response.StockResponse;
import com.n11.bootcamp.stock_service.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/stocks")
@Tag(name = "Stock", description = "Stock management and reservation endpoints")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    @Operation(summary = "List active stock entries with pagination and optional productId filter (admin)")
    public ResponseEntity<ApiResponse<List<StockResponse>>> getAll(
            @RequestParam(name = "productIds", required = false) List<UUID> productIds,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<StockResponse> page = stockService.getAll(productIds, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Stocks fetched"));
    }

    @GetMapping("/product-ids")
    @Operation(summary = "Return UUIDs of all products that have an active stock entry (admin)")
    public ResponseEntity<ApiResponse<List<UUID>>> getStockedProductIds() {
        return ResponseEntity.ok(
                ApiResponse.success(stockService.getAllStockedProductIds(), "Stocked product ids fetched")
        );
    }

    @GetMapping("/availability")
    @Operation(summary = "Batch availability check (used by product-service / cart-service)")
    public ResponseEntity<ApiResponse<List<StockAvailabilityResponse>>> getAvailability(
            @RequestParam("productIds") List<UUID> productIds) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getAvailability(productIds), "Stock Availability fetched"));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get stock info for a product")
    public ResponseEntity<ApiResponse<StockResponse>> getByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getByProductId(productId), "Stock fetched"));
    }

    @GetMapping("/reservations/{orderId}")
    @Operation(summary = "Get all reservations for an order")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getReservationsByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getReservationsByOrderId(orderId), "Reservations fetched"));
    }

    @PostMapping
    @Operation(summary = "Create a new stock entry (admin only)")
    public ResponseEntity<ApiResponse<StockResponse>> createStock(@Valid @RequestBody CreateStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(stockService.createStock(request), "Stock created"));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update stock quantity (admin only)")
    public ResponseEntity<ApiResponse<StockResponse>> updateStock(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateStockRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stockService.updateStock(productId, request), "Stock updated"));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Soft-delete a stock entry (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteStock(@PathVariable UUID productId) {
        stockService.deleteStock(productId);
        return ResponseEntity.ok(ApiResponse.success("Stock deleted"));
    }
}
