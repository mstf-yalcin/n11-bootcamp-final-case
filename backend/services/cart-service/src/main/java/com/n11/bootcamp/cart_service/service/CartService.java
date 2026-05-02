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
import com.n11.bootcamp.cart_service.dto.response.CartItemResponse;
import com.n11.bootcamp.cart_service.dto.response.CartResponse;
import com.n11.bootcamp.cart_service.dto.response.MergeCartResponse;
import com.n11.bootcamp.cart_service.exception.CartItemNotFoundException;
import com.n11.bootcamp.cart_service.exception.CartItemQuantityLimitExceededException;
import com.n11.bootcamp.cart_service.exception.InsufficientStockException;
import com.n11.bootcamp.cart_service.exception.ProductNotFoundException;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartService {

    private static final String KEY_PREFIX = "cart:";
    private static final Duration TTL = Duration.ofDays(30);

    private final RedisTemplate<String, CartData> redisTemplate;
    private final ProductClient productClient;
    private final StockClient stockClient;
    private final int maxQuantityPerItem;

    public CartService(RedisTemplate<String, CartData> redisTemplate,
                       ProductClient productClient,
                       StockClient stockClient,
                       @Value("${app.cart.max-quantity-per-item:10}") int maxQuantityPerItem) {
        this.redisTemplate = redisTemplate;
        this.productClient = productClient;
        this.stockClient = stockClient;
        this.maxQuantityPerItem = maxQuantityPerItem;
    }

    public CartResponse getCart(UUID userId) {
        log.info("Fetching cart: userId={}", userId);
        CartData cart = readCart(userId);

        if (cart.items().isEmpty()) {
            return CartResponse.empty(userId);
        }

        List<UUID> productIds = cart.items().stream().map(CartItemData::productId).toList();
        List<ProductClientResponse> products = fetchProducts(productIds);
        return buildCartResponse(cart, products);
    }

    public CartResponse addItem(UUID userId, AddItemRequest request) {
        log.info("Adding item: userId={}, productId={}, qty={}", userId, request.productId(), request.quantity());
        List<UUID> existing = fetchExistingIds(List.of(request.productId()));
        if (existing.isEmpty()) {
            throw new ProductNotFoundException(request.productId());
        }
        CartData cart = readCart(userId);

        int currentInCart = cart.items().stream()
                .filter(i -> i.productId().equals(request.productId()))
                .mapToInt(CartItemData::quantity)
                .findFirst()
                .orElse(0);
        int desiredTotal = currentInCart + request.quantity();
        ensureWithinCartLimit(request.productId(), desiredTotal);
        ensureStockAvailable(request.productId(), desiredTotal);

        List<CartItemData> items = new ArrayList<>(cart.items());
        boolean exists = false;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(request.productId())) {
                items.set(i, new CartItemData(request.productId(), desiredTotal));
                exists = true;
                break;
            }
        }

        if (!exists) {
            items.add(new CartItemData(request.productId(), request.quantity()));
        }

        CartData updated = new CartData(userId, items, Instant.now());
        saveCart(updated);
        log.info("Item upserted: userId={}, productId={}, exists={}", userId, request.productId(), exists);

        List<ProductClientResponse> products = fetchProducts(items.stream().map(CartItemData::productId).toList());
        return buildCartResponse(updated, products);
    }

    public CartResponse updateItem(UUID userId, UUID productId, UpdateItemRequest request) {
        log.info("Updating item: userId={}, productId={}, qty={}", userId, productId, request.quantity());
        CartData cart = readCart(userId);

        boolean exists = cart.items().stream().anyMatch(i -> i.productId().equals(productId));
        if (!exists) {
            throw new CartItemNotFoundException(productId);
        }

        ensureWithinCartLimit(productId, request.quantity());
        ensureStockAvailable(productId, request.quantity());

        List<CartItemData> items = new ArrayList<>(cart.items());
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(productId)) {
                items.set(i, new CartItemData(productId, request.quantity()));
                break;
            }
        }

        CartData updated = new CartData(userId, items, Instant.now());
        saveCart(updated);

        List<ProductClientResponse> products = fetchProducts(items.stream().map(CartItemData::productId).toList());
        return buildCartResponse(updated, products);
    }

    private void ensureWithinCartLimit(UUID productId, int desiredQuantity) {
        if (desiredQuantity > maxQuantityPerItem) {
            throw new CartItemQuantityLimitExceededException(productId, desiredQuantity, maxQuantityPerItem);
        }
    }

    private void ensureStockAvailable(UUID productId, int desiredQuantity) {
        ApiResponse<List<StockAvailabilityClientResponse>> response = stockClient.getAvailability(List.of(productId));
        StockAvailabilityClientResponse avail = (response != null && response.data() != null)
                ? response.data().stream().findFirst().orElse(null)
                : null;
        int available = avail != null ? avail.available() : 0;
        if (desiredQuantity > available) {
            throw new InsufficientStockException(productId, desiredQuantity, available);
        }
    }

    public CartResponse removeItem(UUID userId, UUID productId) {
        log.info("Removing item: userId={}, productId={}", userId, productId);
        CartData cart = readCart(userId);

        List<CartItemData> items = cart.items().stream()
                .filter(i -> !i.productId().equals(productId))
                .collect(Collectors.toCollection(ArrayList::new));

        if (items.size() == cart.items().size()) {
            throw new CartItemNotFoundException(productId);
        }

        CartData updated = new CartData(userId, items, Instant.now());
        saveCart(updated);

        if (items.isEmpty()) {
            return CartResponse.empty(userId);
        }

        List<ProductClientResponse> products = fetchProducts(items.stream().map(CartItemData::productId).toList());
        return buildCartResponse(updated, products);
    }

    public MergeCartResponse mergeAnonymousCart(UUID userId, MergeRequest request) {
        log.info("Merging anonymous cart: userId={}, anonymousItems={}", userId, request.items().size());

        List<UUID> incomingIds = request.items().stream().map(AddItemRequest::productId).toList();
        List<UUID> existingIds = fetchExistingIds(incomingIds);
        List<UUID> skippedIds = incomingIds.stream().filter(id -> !existingIds.contains(id)).toList();

        if (!skippedIds.isEmpty()) {
            log.warn("Skipping non-existent products during merge: userId={}, skipped={}", userId, skippedIds);
        }

        CartData cart = readCart(userId);
        List<CartItemData> items = new ArrayList<>(cart.items());

        for (AddItemRequest incoming : request.items()) {
            if (!existingIds.contains(incoming.productId())) {
                continue;
            }
            boolean found = false;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).productId().equals(incoming.productId())) {
                    int merged = Math.min(items.get(i).quantity() + incoming.quantity(), maxQuantityPerItem);
                    items.set(i, new CartItemData(incoming.productId(), merged));
                    found = true;
                    break;
                }
            }
            if (!found) {
                int capped = Math.min(incoming.quantity(), maxQuantityPerItem);
                items.add(new CartItemData(incoming.productId(), capped));
            }
        }

        CartData updated = new CartData(userId, items, Instant.now());
        saveCart(updated);
        log.info("Cart merged: userId={}, totalItems={}, skipped={}", userId, items.size(), skippedIds.size());

        CartResponse cartResponse = items.isEmpty()
                ? CartResponse.empty(userId)
                : buildCartResponse(updated, fetchProducts(items.stream().map(CartItemData::productId).toList()));

        return new MergeCartResponse(cartResponse, skippedIds);
    }

    public void clearCart(UUID userId) {
        log.info("Clearing cart: userId={}", userId);
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    private CartData readCart(UUID userId) {
        CartData cart = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return cart != null ? cart : CartData.empty(userId);
    }

    private void saveCart(CartData cart) {
        redisTemplate.opsForValue().set(KEY_PREFIX + cart.userId(), cart, TTL);
    }

    private List<UUID> fetchExistingIds(List<UUID> productIds) {
        var response = productClient.getExistingProductIds(productIds);
        return response != null && response.data() != null ? response.data() : List.of();
    }

    private List<ProductClientResponse> fetchProducts(List<UUID> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        var response = productClient.getProductsByIds(productIds);
        return response != null && response.data() != null ? response.data() : List.of();
    }

    private CartResponse buildCartResponse(CartData cart, List<ProductClientResponse> products) {
        Map<UUID, ProductClientResponse> productMap = products.stream()
                .collect(Collectors.toMap(ProductClientResponse::id, Function.identity()));

        List<CartItemResponse> itemResponses = cart.items().stream()
                .map(item -> {
                    ProductClientResponse product = productMap.get(item.productId());
                    if (product == null) {
                        return new CartItemResponse(
                                item.productId(), null, null,
                                null, null, item.quantity(), null,
                                "UNKNOWN", null);
                    }
                    BigDecimal subtotal = product.price() != null
                            ? product.price().multiply(BigDecimal.valueOf(item.quantity()))
                            : null;
                    return new CartItemResponse(
                            item.productId(),
                            product.name(),
                            product.imageUrl(),
                            product.price(),
                            product.currency(),
                            item.quantity(),
                            subtotal,
                            product.stockStatus() != null ? product.stockStatus() : "UNKNOWN",
                            product.availableQuantity());
                })
                .toList();

        BigDecimal total = itemResponses.stream()
                .filter(i -> i.subtotal() != null)
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = itemResponses.stream()
                .filter(i -> i.currency() != null)
                .map(CartItemResponse::currency)
                .findFirst()
                .orElse("TRY");

        return new CartResponse(cart.userId(), itemResponses, total, currency, cart.updatedAt());
    }
}
