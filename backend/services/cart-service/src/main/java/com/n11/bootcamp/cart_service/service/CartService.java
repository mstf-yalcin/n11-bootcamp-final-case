package com.n11.bootcamp.cart_service.service;

import com.n11.bootcamp.cart_service.client.ProductClient;
import com.n11.bootcamp.cart_service.client.dto.ProductClientResponse;
import com.n11.bootcamp.cart_service.dto.internal.CartData;
import com.n11.bootcamp.cart_service.dto.internal.CartItemData;
import com.n11.bootcamp.cart_service.dto.request.AddItemRequest;
import com.n11.bootcamp.cart_service.dto.request.MergeRequest;
import com.n11.bootcamp.cart_service.dto.request.UpdateItemRequest;
import com.n11.bootcamp.cart_service.dto.response.CartItemResponse;
import com.n11.bootcamp.cart_service.dto.response.CartResponse;
import com.n11.bootcamp.cart_service.dto.response.MergeCartResponse;
import com.n11.bootcamp.cart_service.exception.CartItemNotFoundException;
import com.n11.bootcamp.cart_service.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public CartService(RedisTemplate<String, CartData> redisTemplate, ProductClient productClient) {
        this.redisTemplate = redisTemplate;
        this.productClient = productClient;
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

        List<CartItemData> items = new ArrayList<>(cart.items());
        boolean exists = false;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(request.productId())) {
                items.set(i, new CartItemData(request.productId(), items.get(i).quantity() + request.quantity()));
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

        List<CartItemData> items = new ArrayList<>(cart.items());
        boolean found = false;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(productId)) {
                items.set(i, new CartItemData(productId, request.quantity()));
                found = true;
                break;
            }
        }

        if (!found) {
            throw new CartItemNotFoundException(productId);
        }

        CartData updated = new CartData(userId, items, Instant.now());
        saveCart(updated);

        List<ProductClientResponse> products = fetchProducts(items.stream().map(CartItemData::productId).toList());
        return buildCartResponse(updated, products);
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
                    items.set(i, new CartItemData(incoming.productId(), items.get(i).quantity() + incoming.quantity()));
                    found = true;
                    break;
                }
            }
            if (!found) {
                items.add(new CartItemData(incoming.productId(), incoming.quantity()));
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
                                null, null, item.quantity(), null);
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
                            subtotal);
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
