package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.product_service.dto.request.CreateCategoryRequest;
import com.n11.bootcamp.product_service.dto.request.UpdateCategoryRequest;
import com.n11.bootcamp.product_service.dto.response.CategoryResponse;
import com.n11.bootcamp.product_service.entity.Category;
import com.n11.bootcamp.product_service.exception.CategoryNotFoundException;
import com.n11.bootcamp.product_service.exception.InvalidTargetCategoryException;
import com.n11.bootcamp.product_service.exception.SlugAlreadyExistsException;
import com.n11.bootcamp.product_service.exception.TargetCategoryRequiredException;
import com.n11.bootcamp.product_service.mapper.CategoryMapper;
import com.n11.bootcamp.product_service.repository.CategoryRepository;
import com.n11.bootcamp.product_service.repository.ProductRepository;
import com.n11.bootcamp.product_service.util.SlugUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> getAllCategories() {
        log.info("Listing all active categories");
        return categoryRepository.findAllByIsActiveTrue()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(UUID id) {
        log.info("Fetching category: id={}", id);
        Category category = categoryRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        return categoryMapper.toResponse(category);
    }

    public Optional<Category> findActiveBySlug(String slug) {
        return categoryRepository.findBySlugAndIsActiveTrue(slug);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating category: name={}", request.name());
        Category category = categoryMapper.toEntity(request);
        category.setSlug(generateUniqueSlug(request.name()));
        Category saved = categoryRepository.save(category);
        log.info("Category created: id={}, name={}, slug={}",
                saved.getId(), saved.getName(), saved.getSlug());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        log.info("Updating category: id={}", id);
        Category category = categoryRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        boolean nameChanged = !category.getName().equalsIgnoreCase(request.name());
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        if (nameChanged) {
            category.setSlug(generateUniqueSlug(request.name()));
        }
        Category saved = categoryRepository.save(category);
        log.info("Category updated: id={}, name={}, slug={}",
                saved.getId(), saved.getName(), saved.getSlug());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public void deleteCategory(UUID id, UUID targetCategoryId) {
        log.info("Deleting category (soft): id={}, targetCategoryId={}", id, targetCategoryId);
        Category source = categoryRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        long activeProductCount = productRepository.countByCategoryIdAndIsActiveTrue(id);
        if (activeProductCount > 0) {
            if (targetCategoryId == null) {
                log.warn("Cannot delete category {}: has {} active products and no target provided",
                        id, activeProductCount);
                throw new TargetCategoryRequiredException(activeProductCount);
            }
            if (targetCategoryId.equals(id)) {
                throw new InvalidTargetCategoryException("target must differ from source");
            }
            categoryRepository.findByIdAndIsActiveTrue(targetCategoryId)
                    .orElseThrow(() -> new InvalidTargetCategoryException(
                            "target not found or inactive: " + targetCategoryId));

            int moved = productRepository.bulkMoveActiveProductsToCategory(id, targetCategoryId);
            log.info("Moved {} product(s) from category {} to {}", moved, id, targetCategoryId);
        }

        source.setActive(false);
        categoryRepository.save(source);
        log.info("Category deactivated: id={}", id);
    }

    private String generateUniqueSlug(String name) {
        String slug = SlugUtil.slugifyTurkish(name);
        if (categoryRepository.existsBySlug(slug)) {
            log.warn("Category slug already exists: name={}, slug={}", name, slug);
            throw new SlugAlreadyExistsException(slug);
        }
        return slug;
    }
}
