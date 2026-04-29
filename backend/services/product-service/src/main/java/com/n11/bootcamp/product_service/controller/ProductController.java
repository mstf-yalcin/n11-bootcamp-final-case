package com.n11.bootcamp.product_service.controller;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.product_service.dto.request.CreateProductRequest;
import com.n11.bootcamp.product_service.dto.request.UpdateProductRequest;
import com.n11.bootcamp.product_service.dto.response.ProductResponse;
import com.n11.bootcamp.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management endpoints")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products with optional filtering and pagination")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductResponse> page = productService.getProducts(categoryId, minPrice, maxPrice, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/batch")
    @Operation(summary = "Batch fetch products by IDs (used by cart-service / order-service)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByIds(
            @RequestParam List<UUID> ids) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByIds(ids), "Products fetched"));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get product by slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug), "Product fetched"));
    }

    @PostMapping
    @Operation(summary = "Create a new product (admin only)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productService.createProduct(request), "Product created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product (admin only)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(id, request), "Product updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a product (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted"));
    }
}
