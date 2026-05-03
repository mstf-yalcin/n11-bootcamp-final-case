package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.product_service.dto.request.CreateCategoryRequest;
import com.n11.bootcamp.product_service.dto.request.UpdateCategoryRequest;
import com.n11.bootcamp.product_service.dto.response.CategoryResponse;
import com.n11.bootcamp.product_service.entity.Category;
import com.n11.bootcamp.product_service.exception.CategoryNotFoundException;
import com.n11.bootcamp.product_service.exception.SlugAlreadyExistsException;
import com.n11.bootcamp.product_service.mapper.CategoryMapper;
import com.n11.bootcamp.product_service.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
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
    public void deleteCategory(UUID id) {
        log.info("Deleting category (soft): id={}", id);
        Category category = categoryRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        category.setActive(false);
        categoryRepository.save(category);
        log.info("Category deactivated: id={}", id);
    }

    private String generateUniqueSlug(String name) {
        String slug = slugifyTurkish(name);
        if (categoryRepository.existsBySlug(slug)) {
            log.warn("Category slug already exists: name={}, slug={}", name, slug);
            throw new SlugAlreadyExistsException(slug);
        }
        return slug;
    }

    /** Türkçe ı/ğ/ş/ç/ö/ü → ASCII + slug-safe filter. */
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
