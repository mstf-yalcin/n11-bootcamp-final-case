package com.n11.bootcamp.cart_service.service;

import com.n11.bootcamp.cart_service.client.ProductClient;
import com.n11.bootcamp.cart_service.client.StockClient;
import com.n11.bootcamp.cart_service.client.dto.ProductClientResponse;
import com.n11.bootcamp.cart_service.client.dto.StockAvailabilityClientResponse;
import com.n11.bootcamp.cart_service.dto.internal.CartData;
import com.n11.bootcamp.cart_service.dto.internal.CartItemData;
import com.n11.bootcamp.cart_service.dto.request.AddItemRequest;
import com.n11.bootcamp.cart_service.dto.request.MergeRequest;
import com.n11.bootcamp.cart_service.dto.request.UpdateItemRequest;
import com.n11.bootcamp.cart_service.dto.response.CartResponse;
import com.n11.bootcamp.cart_service.dto.response.MergeCartResponse;
import com.n11.bootcamp.cart_service.exception.CartItemNotFoundException;
import com.n11.bootcamp.cart_service.exception.ProductNotFoundException;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {

    private static final int MAX_QTY_PER_ITEM = 10;

    @Mock
    private RedisTemplate<String, CartData> redisTemplate;

    @Mock
    private ValueOperations<String, CartData> valueOps;

    @Mock
    private ProductClient productClient;

    @Mock
    private StockClient stockClient;

    private CartService cartService;

    private UUID userId;
    private UUID productId;
    private ProductClientResponse product;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        product = new ProductClientResponse(
                productId, "Test Product", BigDecimal.valueOf(100),
                "TRY", "http://img.jpg",
                "IN_STOCK", 100
        );

        cartService = new CartService(redisTemplate, productClient, stockClient, MAX_QTY_PER_ITEM);

        // addItem/updateItem stock check'lerini geçmesi için yeterli stok dön
        when(stockClient.getAvailability(any())).thenReturn(
                new ApiResponse<>(true,
                        List.of(new StockAvailabilityClientResponse(productId, 100, "IN_STOCK")),
                        null, null, null, null)
        );
    }

    private void mockRedis(CartData cart) {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("cart:" + userId)).thenReturn(cart);
    }

    @Test
    void testGetCart_when_cartIsEmpty_returnsEmptyResponse() {
        mockRedis(null);

        CartResponse result = cartService.getCart(userId);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void testGetCart_when_cartHasItems_returnsHydratedResponse() {
        CartData cart = new CartData(userId, List.of(new CartItemData(productId, 2)), Instant.now());
        mockRedis(cart);
        when(productClient.getProductsByIds(List.of(productId)))
                .thenReturn(new ApiResponse<>(true, List.of(product), null, null, null, null));

        CartResponse result = cartService.getCart(userId);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        assertThat(result.items().get(0).productName()).isEqualTo("Test Product");
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void testAddItem_when_productNotInCart_addsNewItem() {
        when(productClient.getExistingProductIds(List.of(productId)))
                .thenReturn(new ApiResponse<>(true, List.of(productId), null, null, null, null));
        mockRedis(null);
        when(productClient.getProductsByIds(any()))
                .thenReturn(new ApiResponse<>(true, List.of(product), null, null, null, null));

        CartResponse result = cartService.addItem(userId, new AddItemRequest(productId, 3));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(3);
        verify(valueOps).set(eq("cart:" + userId), any(CartData.class), eq(Duration.ofDays(30)));
    }

    @Test
    void testAddItem_when_productAlreadyInCart_incrementsQuantity() {
        when(productClient.getExistingProductIds(List.of(productId)))
                .thenReturn(new ApiResponse<>(true, List.of(productId), null, null, null, null));
        CartData cart = new CartData(userId, new ArrayList<>(List.of(new CartItemData(productId, 1))), Instant.now());
        mockRedis(cart);
        when(productClient.getProductsByIds(any()))
                .thenReturn(new ApiResponse<>(true, List.of(product), null, null, null, null));

        CartResponse result = cartService.addItem(userId, new AddItemRequest(productId, 2));

        assertThat(result.items().get(0).quantity()).isEqualTo(3);
    }

    @Test
    void testAddItem_when_productNotFound_throwsException() {
        when(productClient.getExistingProductIds(List.of(productId)))
                .thenReturn(new ApiResponse<>(true, List.of(), null, null, null, null));

        assertThatThrownBy(() -> cartService.addItem(userId, new AddItemRequest(productId, 1)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void testUpdateItem_when_itemExists_updatesQuantity() {
        CartData cart = new CartData(userId, new ArrayList<>(List.of(new CartItemData(productId, 5))), Instant.now());
        mockRedis(cart);
        when(productClient.getProductsByIds(any()))
                .thenReturn(new ApiResponse<>(true, List.of(product), null, null, null, null));

        CartResponse result = cartService.updateItem(userId, productId, new UpdateItemRequest(10));

        assertThat(result.items().get(0).quantity()).isEqualTo(10);
    }

    @Test
    void testUpdateItem_when_itemNotFound_throwsException() {
        mockRedis(null);

        assertThatThrownBy(() -> cartService.updateItem(userId, productId, new UpdateItemRequest(1)))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void testRemoveItem_when_itemExists_removesFromCart() {
        CartData cart = new CartData(userId, new ArrayList<>(List.of(new CartItemData(productId, 2))), Instant.now());
        mockRedis(cart);

        CartResponse result = cartService.removeItem(userId, productId);

        assertThat(result.items()).isEmpty();
    }

    @Test
    void testRemoveItem_when_itemNotFound_throwsException() {
        mockRedis(null);

        assertThatThrownBy(() -> cartService.removeItem(userId, productId))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void testMergeAnonymousCart_when_cartEmpty_addsAllItems() {
        when(productClient.getExistingProductIds(List.of(productId)))
                .thenReturn(new ApiResponse<>(true, List.of(productId), null, null, null, null));
        mockRedis(null);
        when(productClient.getProductsByIds(any()))
                .thenReturn(new ApiResponse<>(true, List.of(product), null, null, null, null));

        MergeCartResponse result = cartService.mergeAnonymousCart(userId,
                new MergeRequest(List.of(new AddItemRequest(productId, 2))));

        assertThat(result.cart().items()).hasSize(1);
        assertThat(result.cart().items().get(0).quantity()).isEqualTo(2);
        assertThat(result.skippedProductIds()).isEmpty();
    }

    @Test
    void testMergeAnonymousCart_when_itemAlreadyExists_mergesQuantities() {
        when(productClient.getExistingProductIds(List.of(productId)))
                .thenReturn(new ApiResponse<>(true, List.of(productId), null, null, null, null));
        CartData cart = new CartData(userId, new ArrayList<>(List.of(new CartItemData(productId, 3))), Instant.now());
        mockRedis(cart);
        when(productClient.getProductsByIds(any()))
                .thenReturn(new ApiResponse<>(true, List.of(product), null, null, null, null));

        MergeCartResponse result = cartService.mergeAnonymousCart(userId,
                new MergeRequest(List.of(new AddItemRequest(productId, 2))));

        assertThat(result.cart().items().get(0).quantity()).isEqualTo(5);
        assertThat(result.skippedProductIds()).isEmpty();
    }

    @Test
    void testMergeAnonymousCart_when_someProductsNotFound_skipsInvalidOnes() {
        UUID invalidId = UUID.randomUUID();
        when(productClient.getExistingProductIds(List.of(productId, invalidId)))
                .thenReturn(new ApiResponse<>(true, List.of(productId), null, null, null, null));
        mockRedis(null);
        when(productClient.getProductsByIds(any()))
                .thenReturn(new ApiResponse<>(true, List.of(product), null, null, null, null));

        MergeCartResponse result = cartService.mergeAnonymousCart(userId, new MergeRequest(List.of(
                new AddItemRequest(productId, 2),
                new AddItemRequest(invalidId, 1)
        )));

        assertThat(result.cart().items()).hasSize(1);
        assertThat(result.skippedProductIds()).containsExactly(invalidId);
    }

    @Test
    void testClearCart_deletesRedisKey() {
        cartService.clearCart(userId);

        verify(redisTemplate).delete("cart:" + userId);
    }
}
