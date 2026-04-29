package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.product_service.dto.request.CreateCategoryRequest;
import com.n11.bootcamp.product_service.dto.response.CategoryResponse;
import com.n11.bootcamp.product_service.entity.Category;
import com.n11.bootcamp.product_service.exception.CategoryNotFoundException;
import com.n11.bootcamp.product_service.mapper.CategoryMapper;
import com.n11.bootcamp.product_service.repository.CategoryRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
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

        categoryResponse = new CategoryResponse(categoryId, "Electronics", "Electronic products", null, null);
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
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Electronic products");

        when(categoryMapper.toEntity(request)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        CategoryResponse result = categoryService.createCategory(request);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Electronics");
        verify(categoryRepository).save(category);
    }

    @Test
    void testDeleteCategory_when_categoryExists_setsIsActiveFalse() {
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(categoryId);

        assertThat(category.isActive()).isFalse();
        verify(categoryRepository).save(category);
    }

    @Test
    void testDeleteCategory_when_categoryNotFound_throwsCategoryNotFoundException() {
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
