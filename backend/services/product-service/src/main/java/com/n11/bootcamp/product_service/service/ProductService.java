package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.product_service.dto.request.CreateProductRequest;
import com.n11.bootcamp.product_service.dto.request.UpdateProductRequest;
import com.n11.bootcamp.product_service.dto.response.ProductResponse;
import com.n11.bootcamp.product_service.entity.Category;
import com.n11.bootcamp.product_service.entity.Product;
import com.n11.bootcamp.product_service.entity.Tag;
import com.n11.bootcamp.product_service.exception.CategoryNotFoundException;
import com.n11.bootcamp.product_service.exception.ProductNotFoundException;
import com.n11.bootcamp.product_service.exception.SlugGenerationException;
import com.n11.bootcamp.product_service.mapper.ProductMapper;
import com.n11.bootcamp.product_service.repository.CategoryRepository;
import com.n11.bootcamp.product_service.repository.ProductRepository;
import com.n11.bootcamp.product_service.repository.TagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, TagRepository tagRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.productMapper = productMapper;
    }

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

    public List<UUID> getExistingProductIds(List<UUID> ids) {
        log.info("Checking existing products: ids={}", ids);
        return productRepository.findExistingIdsByIdIn(ids);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product: name={}, category={}", request.name(), request.categoryId());
        Category category = categoryRepository.findByIdAndIsActiveTrue(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setSlug(generateUniqueSlug(request.name()));
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

        productMapper.updateEntity(request, product);
        product.setCategory(category);
        if (request.currency() != null) {
            product.setCurrency(request.currency());
        }
        if (request.tagIds() != null) {
            product.setTags(resolveTags(request.tagIds()));
        }

        Product saved = productRepository.save(product);
        log.info("Product updated: id={}, slug={} (unchanged)", saved.getId(), saved.getSlug());
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

    private String generateUniqueSlug(String name) {
        String baseSlug = slugifyTurkish(name);

        for (int i = 0; i < 5; i++) {
            String slug = baseSlug + "-" + ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000);
            if (!productRepository.existsBySlug(slug)) {
                return slug;
            }
        }
        throw new SlugGenerationException(name);
    }

    /** Türkçe ı/ğ/ş/ç/ö/ü  + Unicode NFD normalize + slug-safe filter. */
    private String slugifyTurkish(String input) {
        String tr = input
                .replace('ı', 'i').replace('İ', 'I')
                .replace('ğ', 'g').replace('Ğ', 'G')
                .replace('ş', 's').replace('Ş', 'S')
                .replace('ö', 'o').replace('Ö', 'O')
                .replace('ü', 'u').replace('Ü', 'U')
                .replace('ç', 'c').replace('Ç', 'C');
        return Normalizer.normalize(tr, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
