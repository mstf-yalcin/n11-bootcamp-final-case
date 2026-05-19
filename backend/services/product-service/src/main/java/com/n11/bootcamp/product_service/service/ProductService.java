package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.product_service.dto.request.CreateProductRequest;
import com.n11.bootcamp.product_service.dto.request.UpdateProductRequest;
import com.n11.bootcamp.product_service.dto.response.ProductMinimalResponse;
import com.n11.bootcamp.product_service.dto.response.ProductResponse;
import com.n11.bootcamp.product_service.dto.response.SearchSuggestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Public contract for product domain. Controllers depend on this interface only.
 *
 * <p>Two implementations are wired via {@code app.product.search.engine} flag:</p>
 * <ul>
 *   <li>{@code jpa} (default) → {@link JpaProductService} — all reads and writes hit PostgreSQL.</li>
 *   <li>{@code elastic} → {@link ElasticProductService} — list / search reads hit Elasticsearch
 *       (filter only), single-item reads and all writes delegate to {@link JpaProductService}.</li>
 * </ul>
 */
public interface ProductService {

    Page<ProductResponse> getProducts(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                      BigDecimal minRating, String search, Pageable pageable);

    Page<ProductResponse> getAdminProducts(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                           BigDecimal minRating, String search, boolean includeInactive,
                                           Pageable pageable);

    ProductResponse getProductById(UUID id);

    ProductResponse getProductBySlug(String slug);

    List<ProductMinimalResponse> getProductsByIds(List<UUID> ids);

    List<UUID> getExistingProductIds(List<UUID> ids);

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(UUID id, UpdateProductRequest request);

    void deleteProduct(UUID id);

    ProductResponse restoreProduct(UUID id);

    SearchSuggestionResponse suggest(String query);
}
