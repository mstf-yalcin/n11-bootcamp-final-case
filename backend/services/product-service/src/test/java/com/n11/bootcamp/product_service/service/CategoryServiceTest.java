package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.product_service.dto.request.CreateCategoryRequest;
import com.n11.bootcamp.product_service.dto.response.CategoryResponse;
import com.n11.bootcamp.product_service.entity.Category;
import com.n11.bootcamp.product_service.exception.CategoryNotFoundException;
import com.n11.bootcamp.product_service.exception.InvalidTargetCategoryException;
import com.n11.bootcamp.product_service.exception.TargetCategoryRequiredException;
import com.n11.bootcamp.product_service.mapper.CategoryMapper;
import com.n11.bootcamp.product_service.repository.CategoryRepository;
import com.n11.bootcamp.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private UUID categoryId;
    private Category category;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();

        category = new Category();
        category.setName("Electronics");
        category.setDescription("Electronic products");
        category.setImageUrl("https://example.com/electronics.jpg");

        categoryResponse = new CategoryResponse(categoryId, "Electronics", "electronics", "Electronic products", "https://example.com/electronics.jpg", null, null);
    }

    @Test
    void testGetCategoryById_when_categoryExists_returnsCategoryResponse() {
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        CategoryResponse result = categoryService.getCategoryById(categoryId);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Electronics");
        verify(categoryRepository).findByIdAndIsActiveTrue(categoryId);
    }

    @Test
    void testGetCategoryById_when_categoryNotFound_throwsCategoryNotFoundException() {
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void testCreateCategory_when_requestValid_returnsSavedCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Electronic products", "https://example.com/electronics.jpg");

        when(categoryMapper.toEntity(request)).thenReturn(category);
        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        CategoryResponse result = categoryService.createCategory(request);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Electronics");
        assertThat(category.getSlug()).isEqualTo("electronics");
        verify(categoryRepository).save(category);
    }

    @Test
    void testCreateCategory_when_slugCollision_throwsSlugAlreadyExistsException() {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Electronic products", "https://example.com/electronics.jpg");

        when(categoryMapper.toEntity(request)).thenReturn(category);
        when(categoryRepository.existsBySlug("electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(com.n11.bootcamp.product_service.exception.SlugAlreadyExistsException.class);
    }

    @Test
    void testDeleteCategory_when_noActiveProducts_setsIsActiveFalse() {
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryIdAndIsActiveTrue(categoryId)).thenReturn(0L);

        categoryService.deleteCategory(categoryId, null);

        assertThat(category.isActive()).isFalse();
        verify(categoryRepository).save(category);
        verify(productRepository, never()).bulkMoveActiveProductsToCategory(any(), any());
    }

    @Test
    void testDeleteCategory_when_categoryNotFound_throwsCategoryNotFoundException() {
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, null))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void testDeleteCategory_when_hasActiveProductsAndNoTarget_throwsTargetCategoryRequiredException() {
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryIdAndIsActiveTrue(categoryId)).thenReturn(5L);

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, null))
                .isInstanceOf(TargetCategoryRequiredException.class);

        assertThat(category.isActive()).isTrue();
        verify(categoryRepository, never()).save(category);
        verify(productRepository, never()).bulkMoveActiveProductsToCategory(any(), any());
    }

    @Test
    void testDeleteCategory_when_targetEqualsSource_throwsInvalidTargetCategoryException() {
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryIdAndIsActiveTrue(categoryId)).thenReturn(3L);

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, categoryId))
                .isInstanceOf(InvalidTargetCategoryException.class);

        verify(productRepository, never()).bulkMoveActiveProductsToCategory(any(), any());
    }

    @Test
    void testDeleteCategory_when_targetInactive_throwsInvalidTargetCategoryException() {
        UUID targetId = UUID.randomUUID();
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryIdAndIsActiveTrue(categoryId)).thenReturn(2L);
        when(categoryRepository.findByIdAndIsActiveTrue(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, targetId))
                .isInstanceOf(InvalidTargetCategoryException.class);

        verify(productRepository, never()).bulkMoveActiveProductsToCategory(any(), any());
    }

    @Test
    void testDeleteCategory_when_hasActiveProductsAndValidTarget_movesAndDeactivates() {
        UUID targetId = UUID.randomUUID();
        Category target = new Category();
        target.setName("Target");

        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryIdAndIsActiveTrue(categoryId)).thenReturn(4L);
        when(categoryRepository.findByIdAndIsActiveTrue(targetId)).thenReturn(Optional.of(target));
        when(productRepository.bulkMoveActiveProductsToCategory(categoryId, targetId)).thenReturn(4);

        categoryService.deleteCategory(categoryId, targetId);

        assertThat(category.isActive()).isFalse();
        verify(productRepository).bulkMoveActiveProductsToCategory(categoryId, targetId);
        verify(categoryRepository).save(category);
    }
}
