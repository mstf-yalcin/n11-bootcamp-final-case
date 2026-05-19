package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.product_service.client.StockClient;
import com.n11.bootcamp.product_service.client.dto.StockAvailabilityClientResponse;
import com.n11.bootcamp.product_service.dto.request.CreateProductRequest;
import com.n11.bootcamp.product_service.dto.request.UpdateProductRequest;
import com.n11.bootcamp.product_service.dto.response.ProductMinimalResponse;
import com.n11.bootcamp.product_service.dto.response.ProductResponse;
import com.n11.bootcamp.product_service.dto.response.SearchSuggestionResponse;
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
import com.n11.bootcamp.product_service.util.SlugUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class JpaProductService implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ProductMapper productMapper;
    private final StockClient stockClient;
    private final StockAvailabilityCache stockAvailabilityCache;

    public JpaProductService(ProductRepository productRepository,
                             CategoryRepository categoryRepository,
                             TagRepository tagRepository,
                             ProductMapper productMapper,
                             StockClient stockClient,
                             StockAvailabilityCache stockAvailabilityCache) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.productMapper = productMapper;
        this.stockClient = stockClient;
        this.stockAvailabilityCache = stockAvailabilityCache;
    }

    @Override
    public Page<ProductResponse> getProducts(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                             BigDecimal minRating, String search, Pageable pageable) {
        log.info("JPA listing products: categoryId={}, minPrice={}, maxPrice={}, minRating={}, search={}, page={}",
                categoryId, minPrice, maxPrice, minRating, search, pageable.getPageNumber());
        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search.toLowerCase() + "%"
                : null;

        Page<UUID> idsPage = productRepository.findIdsWithFilters(
                categoryId, minPrice, maxPrice, minRating, searchPattern, false, pageable);

        if (idsPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idsPage.getTotalElements());
        }

        Page<ProductResponse> page = fetchAndOrderByIds(idsPage, false);
        return enrichPageWithStock(page);
    }

    @Override
    public Page<ProductResponse> getAdminProducts(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                                  BigDecimal minRating, String search, boolean includeInactive,
                                                  Pageable pageable) {
        log.info("JPA admin listing products: categoryId={}, minPrice={}, maxPrice={}, minRating={}, search={}, includeInactive={}, page={}",
                categoryId, minPrice, maxPrice, minRating, search, includeInactive, pageable.getPageNumber());
        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search.toLowerCase() + "%"
                : null;

        Page<UUID> idsPage = productRepository.findIdsWithFilters(
                categoryId, minPrice, maxPrice, minRating, searchPattern, includeInactive, pageable);

        if (idsPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idsPage.getTotalElements());
        }

        Page<ProductResponse> page = fetchAndOrderByIds(idsPage, includeInactive);
        return enrichPageWithStock(page);
    }

    private Page<ProductResponse> fetchAndOrderByIds(Page<UUID> idsPage, boolean includeInactive) {
        List<UUID> ids = idsPage.getContent();
        Map<UUID, Product> byId = productRepository.findByIdInWithTags(ids, includeInactive).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<ProductResponse> ordered = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(productMapper::toResponse)
                .toList();

        return new PageImpl<>(ordered, idsPage.getPageable(), idsPage.getTotalElements());
    }

    @Override
    @Transactional
    public ProductResponse restoreProduct(UUID id) {
        log.info("Restoring product: id={}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (product.isActive()) {
            log.info("Product already active, no-op: id={}", id);
        } else {
            product.setActive(true);
            productRepository.save(product);
            log.info("Product restored: id={}", id);
        }
        return enrichWithStock(productMapper.toResponse(product));
    }

    @Override
    public ProductResponse getProductById(UUID id) {
        log.info("Fetching product: id={}", id);
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return enrichWithStock(productMapper.toResponse(product));
    }

    @Override
    public ProductResponse getProductBySlug(String slug) {
        log.info("Fetching product: slug={}", slug);
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ProductNotFoundException(slug));
        return enrichWithStock(productMapper.toResponse(product));
    }

    @Override
    public List<ProductMinimalResponse> getProductsByIds(List<UUID> ids) {
        log.info("Batch fetching minimal products: ids={}", ids);
        List<ProductMinimalResponse> projected = productRepository.findMinimalByIds(ids);
        return enrichMinimalWithStock(projected);
    }

    @Override
    public List<UUID> getExistingProductIds(List<UUID> ids) {
        log.info("Checking existing products: ids={}", ids);
        return productRepository.findExistingIdsByIdIn(ids);
    }

    @Override
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

    @Override
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

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        log.info("Deleting product (soft): id={}", id);
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Product deactivated: id={}", id);
    }

    // ---------------------------------------------------------------------
    // Internal helpers — also reused by ElasticProductService via composition.
    // ---------------------------------------------------------------------

    Page<ProductResponse> enrichPageWithStock(Page<ProductResponse> page) {
        List<UUID> ids = page.getContent().stream().map(ProductResponse::id).toList();
        Map<UUID, StockAvailabilityClientResponse> stockMap = stockAvailabilityCache.getAvailabilityMap(ids);
        return page.map(p -> {
            StockAvailabilityClientResponse avail = stockMap.get(p.id());
            if (avail == null) {
                return p.withStock("UNKNOWN", null);
            }
            return p.withStock(avail.status(), avail.available());
        });
    }

    ProductResponse enrichWithStock(ProductResponse response) {
        Map<UUID, StockAvailabilityClientResponse> map = fetchAvailability(List.of(response.id()));
        StockAvailabilityClientResponse avail = map.get(response.id());
        if (avail == null) {
            return response.withStock("UNKNOWN", null);
        }
        return response.withStock(avail.status(), avail.available());
    }

    List<ProductMinimalResponse> enrichMinimalWithStock(List<ProductMinimalResponse> responses) {
        if (responses.isEmpty()) {
            return responses;
        }
        List<UUID> ids = responses.stream().map(ProductMinimalResponse::id).toList();
        Map<UUID, StockAvailabilityClientResponse> map = fetchAvailability(ids);
        return responses.stream()
                .map(r -> {
                    StockAvailabilityClientResponse avail = map.get(r.id());
                    if (avail == null) {
                        return r.withStock("UNKNOWN", null);
                    }
                    return r.withStock(avail.status(), avail.available());
                })
                .toList();
    }

    private Map<UUID, StockAvailabilityClientResponse> fetchAvailability(List<UUID> ids) {
        ApiResponse<List<StockAvailabilityClientResponse>> response = stockClient.getAvailability(ids);
        if (response == null || response.data() == null || response.data().isEmpty()) {
            return new HashMap<>();
        }
        Map<UUID, StockAvailabilityClientResponse> map = new HashMap<>();
        for (StockAvailabilityClientResponse item : response.data()) {
            map.put(item.productId(), item);
        }
        return map;
    }

    @Override
    public SearchSuggestionResponse suggest(String query) {
        return new SearchSuggestionResponse(query, null);
    }

    ProductMapper getProductMapper() {
        return productMapper;
    }

    ProductRepository getProductRepository() {
        return productRepository;
    }

    private Set<Tag> resolveTags(Set<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(tagRepository.findAllById(tagIds));
    }

    private String generateUniqueSlug(String name) {
        String baseSlug = SlugUtil.slugifyTurkish(name);

        for (int i = 0; i < 5; i++) {
            String slug = baseSlug + "-" + ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000);
            if (!productRepository.existsBySlug(slug)) {
                return slug;
            }
        }
        throw new SlugGenerationException(name);
    }
}
