package com.n11.bootcamp.product_service.controller;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.product_service.dto.response.ProductResponse;
import com.n11.bootcamp.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@Tag(name = "Admin Products", description = "Admin endpoints for managing products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List products including inactive ones (admin only)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> listProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "true") boolean includeInactive,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> page = productService.getAdminProducts(
                categoryId, minPrice, maxPrice, minRating, search, includeInactive, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted product (admin only)")
    public ResponseEntity<ApiResponse<ProductResponse>> restoreProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(productService.restoreProduct(id), "Product restored"));
    }
}
