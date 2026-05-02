package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.product_service.client.StockClient;
import com.n11.bootcamp.product_service.dto.request.CreateProductRequest;
import com.n11.bootcamp.product_service.dto.request.UpdateProductRequest;
import com.n11.bootcamp.product_service.dto.response.ProductResponse;
import com.n11.bootcamp.product_service.entity.Category;
import com.n11.bootcamp.product_service.entity.Product;
import com.n11.bootcamp.product_service.exception.CategoryNotFoundException;
import com.n11.bootcamp.product_service.exception.ProductNotFoundException;
import com.n11.bootcamp.product_service.mapper.ProductMapper;
import com.n11.bootcamp.product_service.repository.CategoryRepository;
import com.n11.bootcamp.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private StockClient stockClient;
    @Mock
    private StockAvailabilityCache stockAvailabilityCache;

    @InjectMocks
    private ProductService productService;

    private UUID productId;
    private UUID categoryId;
    private Category category;
    private Product product;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = new Category();
        category.setName("Electronics");

        product = new Product();
        product.setSlug("headphones");
        product.setName("Headphones");
        product.setPrice(BigDecimal.valueOf(999.99));
        product.setCategory(category);

        productResponse = new ProductResponse(
                productId, "headphones", "Headphones", null,
                BigDecimal.valueOf(999.99), "TRY", 0, BigDecimal.ZERO,
                null, Set.of(), categoryId, "Electronics", null, null,
                null, null, true
        );
    }

    @Test
    void testGetProductById_when_productExists_returnsProductResponse() {
        when(productRepository.findByIdAndIsActiveTrue(productId)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        ProductResponse result = productService.getProductById(productId);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Headphones");
        verify(productRepository).findByIdAndIsActiveTrue(productId);
    }

    @Test
    void testGetProductById_when_productNotFound_throwsProductNotFoundException() {
        when(productRepository.findByIdAndIsActiveTrue(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void testGetProductBySlug_when_productExists_returnsProductResponse() {
        when(productRepository.findBySlugAndIsActiveTrue("headphones")).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        ProductResponse result = productService.getProductBySlug("headphones");

        assertThat(result).isNotNull();
        assertThat(result.slug()).isEqualTo("headphones");
        verify(productRepository).findBySlugAndIsActiveTrue("headphones");
    }

    @Test
    void testGetProductBySlug_when_productNotFound_throwsProductNotFoundException() {
        when(productRepository.findBySlugAndIsActiveTrue("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductBySlug("nonexistent"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void testGetProductsByIds_when_idsProvided_returnsList() {
        when(productRepository.findAllByIdInAndIsActiveTrue(List.of(productId))).thenReturn(List.of(product));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        List<ProductResponse> result = productService.getProductsByIds(List.of(productId));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).slug()).isEqualTo("headphones");
    }

    @Test
    void testCreateProduct_when_categoryExists_returnsCreatedProduct() {
        CreateProductRequest request = new CreateProductRequest(
                "Headphones", "Great sound", BigDecimal.valueOf(999.99), null, null, null, categoryId
        );

        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        ProductResponse result = productService.createProduct(request);

        assertThat(result).isNotNull();
        verify(productRepository).save(product);
    }

    @Test
    void testCreateProduct_when_categoryNotFound_throwsCategoryNotFoundException() {
        CreateProductRequest request = new CreateProductRequest(
                "Headphones", null, BigDecimal.valueOf(999.99), null, null, null, categoryId
        );

        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void testDeleteProduct_when_productExists_setsIsActiveFalse() {
        when(productRepository.findByIdAndIsActiveTrue(productId)).thenReturn(Optional.of(product));

        productService.deleteProduct(productId);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void testDeleteProduct_when_productNotFound_throwsProductNotFoundException() {
        when(productRepository.findByIdAndIsActiveTrue(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void testUpdateProduct_when_productAndCategoryExist_returnsUpdatedProduct() {
        UpdateProductRequest request = new UpdateProductRequest(
                "Updated Headphones", "Even better", BigDecimal.valueOf(1099.99), null, null, null, categoryId
        );

        when(productRepository.findByIdAndIsActiveTrue(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findByIdAndIsActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        ProductResponse result = productService.updateProduct(productId, request);

        assertThat(result).isNotNull();
        verify(productMapper).updateEntity(request, product);
    }
}
