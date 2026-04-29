package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.product_service.dto.request.CreateProductRequest;
import com.n11.bootcamp.product_service.dto.request.UpdateProductRequest;
import com.n11.bootcamp.product_service.dto.response.ProductResponse;
import com.n11.bootcamp.product_service.entity.Category;
import com.n11.bootcamp.product_service.entity.Product;
import com.n11.bootcamp.product_service.entity.Tag;
import com.n11.bootcamp.product_service.exception.CategoryNotFoundException;
import com.n11.bootcamp.product_service.exception.ProductNotFoundException;
import com.n11.bootcamp.product_service.exception.SlugAlreadyExistsException;
import com.n11.bootcamp.product_service.mapper.ProductMapper;
import com.n11.bootcamp.product_service.repository.CategoryRepository;
import com.n11.bootcamp.product_service.repository.ProductRepository;
import com.n11.bootcamp.product_service.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ProductMapper productMapper;

    public Page<ProductResponse> getProducts(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                             String search, Pageable pageable) {
        log.info("Listing products: categoryId={}, minPrice={}, maxPrice={}, search={}, page={}",
                categoryId, minPrice, maxPrice, search, pageable.getPageNumber());
        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search.toLowerCase() + "%"
                : null;
        return productRepository.findWithFilters(categoryId, minPrice, maxPrice, searchPattern, pageable)
                .map(productMapper::toResponse);
    }

    public ProductResponse getProductById(UUID id) {
        log.info("Fetching product: id={}", id);
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toResponse(product);
    }

    public ProductResponse getProductBySlug(String slug) {
        log.info("Fetching product: slug={}", slug);
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ProductNotFoundException(slug));
        return productMapper.toResponse(product);
    }

    public List<ProductResponse> getProductsByIds(List<UUID> ids) {
        log.info("Batch fetching products: ids={}", ids);
        return productRepository.findAllByIdInAndIsActiveTrue(ids)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product: name={}, category={}", request.name(), request.categoryId());
        Category category = categoryRepository.findByIdAndIsActiveTrue(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

        String slug = generateSlug(request.name());
        if (productRepository.existsBySlug(slug)) {
            throw new SlugAlreadyExistsException(slug);
        }

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setSlug(slug);
        product.setCurrency(request.currency() != null ? request.currency() : "TRY");
        product.setRatingCount(0);
        product.setRatingAverage(BigDecimal.ZERO);
        product.setTags(resolveTags(request.tagIds()));

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, slug={}", saved.getId(), saved.getSlug());
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        log.info("Updating product: id={}", id);
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        Category category = categoryRepository.findByIdAndIsActiveTrue(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

        String slug = generateSlug(request.name());
        if (!slug.equals(product.getSlug()) && productRepository.existsBySlug(slug)) {
            throw new SlugAlreadyExistsException(slug);
        }

        productMapper.updateEntity(request, product);
        product.setCategory(category);
        product.setSlug(slug);
        if (request.currency() != null) {
            product.setCurrency(request.currency());
        }
        if (request.tagIds() != null) {
            product.setTags(resolveTags(request.tagIds()));
        }

        Product saved = productRepository.save(product);
        log.info("Product updated: id={}, slug={}", saved.getId(), saved.getSlug());
        return productMapper.toResponse(saved);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        log.info("Deleting product (soft): id={}", id);
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Product deactivated: id={}", id);
    }

    private Set<Tag> resolveTags(Set<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(tagRepository.findAllById(tagIds));
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
