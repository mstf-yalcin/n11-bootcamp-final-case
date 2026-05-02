package com.n11.bootcamp.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.common_lib.event.AggregateType;
import com.n11.bootcamp.common_lib.event.EventType;
import com.n11.bootcamp.common_lib.event.order.BuyerInfo;
import com.n11.bootcamp.common_lib.event.order.CancelReason;
import com.n11.bootcamp.common_lib.event.order.OrderCancelledPayload;
import com.n11.bootcamp.common_lib.event.order.OrderConfirmedPayload;
import com.n11.bootcamp.common_lib.event.order.OrderCreatedPayload;
import com.n11.bootcamp.common_lib.event.order.OrderEventItem;
import com.n11.bootcamp.common_lib.event.order.OrderStatus;
import com.n11.bootcamp.common_lib.event.order.ShippingAddress;
import com.n11.bootcamp.order_service.client.CartClient;
import com.n11.bootcamp.order_service.client.ProductClient;
import com.n11.bootcamp.order_service.client.UserClient;
import com.n11.bootcamp.order_service.client.dto.AddressClientResponse;
import com.n11.bootcamp.order_service.client.dto.CartClientResponse;
import com.n11.bootcamp.order_service.client.dto.CartItemClientResponse;
import com.n11.bootcamp.order_service.client.dto.CheckoutContextClientResponse;
import com.n11.bootcamp.order_service.client.dto.ProductClientResponse;
import com.n11.bootcamp.order_service.client.dto.UserInfoClientResponse;
import com.n11.bootcamp.order_service.dto.request.AdminUpdateOrderStatusRequest;
import com.n11.bootcamp.order_service.dto.request.CreateOrderRequest;
import com.n11.bootcamp.order_service.dto.response.OrderResponse;
import com.n11.bootcamp.order_service.entity.Order;
import com.n11.bootcamp.order_service.entity.OrderItem;
import com.n11.bootcamp.order_service.entity.OutboxEvent;
import com.n11.bootcamp.order_service.exception.*;
import com.n11.bootcamp.order_service.mapper.OrderMapper;
import com.n11.bootcamp.order_service.repository.OrderRepository;
import com.n11.bootcamp.order_service.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final String DEFAULT_CURRENCY = "TRY";
    private static final String AGGREGATE_TYPE = AggregateType.ORDER;

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final ProductClient productClient;
    private final CartClient cartClient;
    private final UserClient userClient;

    public OrderService(OrderRepository orderRepository,
                        OutboxEventRepository outboxEventRepository,
                        OrderMapper orderMapper,
                        ObjectMapper objectMapper,
                        ProductClient productClient,
                        CartClient cartClient,
                        UserClient userClient) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.productClient = productClient;
        this.cartClient = cartClient;
        this.userClient = userClient;
    }

    public OrderResponse getOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserIdAndIsActiveTrue(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponse(order);
    }

    public Page<OrderResponse> listOrders(UUID userId, Pageable pageable) {
        return orderRepository
                .findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId, pageable)
                .map(orderMapper::toResponse);
    }

    public Page<OrderResponse> searchAdminOrders(OrderStatus status, UUID userId,
                                                 Instant from, Instant to,
                                                 String search, Pageable pageable) {
        String pattern = (search != null && !search.isBlank())
                ? "%" + search.toLowerCase().trim() + "%"
                : null;
        UUID searchUuid = tryParseUuid(search);
        return orderRepository
                .searchAdminOrders(status, userId, from, to, pattern, searchUuid, pageable)
                .map(orderMapper::toResponse);
    }

    private static UUID tryParseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public OrderResponse getAdminOrder(UUID orderId) {
        Order order = orderRepository.findByIdAndIsActiveTrue(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse adminCancelOrder(UUID orderId, String note) {
        Order order = orderRepository.findByIdAndIsActiveTrue(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus status = order.getStatus();
        boolean cancellable = status == OrderStatus.PENDING
                || status == OrderStatus.STOCK_RESERVED
                || status == OrderStatus.CONFIRMED;
        if (!cancellable) {
            throw new InvalidOrderStateException(status, "admin-cancel");
        }

        String correlationId = MDC.get("correlationId");
        cancelOrderInternal(order, CancelReason.ADMIN_CANCELLED, correlationId);
        log.info("Admin cancelled order: orderId={}, prevStatus={}, note={}, correlationId={}",
                orderId, status, note, correlationId);
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse adminUpdateStatus(UUID orderId, AdminUpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdAndIsActiveTrue(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus current = order.getStatus();
        OrderStatus next = request.status();
        String correlationId = MDC.get("correlationId");

        if (current == next) {
            log.info("Admin status update is a no-op: orderId={}, status={}", orderId, current);
            return orderMapper.toResponse(order);
        }

        if (next == OrderStatus.SHIPPED && current == OrderStatus.CONFIRMED) {
            order.setStatus(OrderStatus.SHIPPED);
        } else if (next == OrderStatus.DELIVERED && current == OrderStatus.SHIPPED) {
            order.setStatus(OrderStatus.DELIVERED);
        } else if (next == OrderStatus.CANCELLED
                && (current == OrderStatus.PENDING
                    || current == OrderStatus.STOCK_RESERVED
                    || current == OrderStatus.CONFIRMED)) {
            cancelOrderInternal(order, CancelReason.ADMIN_CANCELLED, correlationId);
        } else {
            throw new InvalidOrderStateException(current, "admin-transition-to-" + next);
        }

        log.info("Admin updated order status: orderId={}, from={}, to={}, note={}, correlationId={}",
                orderId, current, next, request.note(), correlationId);
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(UUID userId, String userEmail, String ip, CreateOrderRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Creating order: userId={}, addressId={}, correlationId={}",
                userId, request.addressId(), correlationId);

        CartClientResponse cart = fetchCart();
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw new EmptyCartException();
        }

        List<UUID> productIds = cart.items().stream()
                .map(CartItemClientResponse::productId)
                .toList();
        Map<UUID, ProductClientResponse> productMap = fetchProducts(productIds);

        List<UUID> missing = productIds.stream()
                .filter(id -> !productMap.containsKey(id))
                .toList();
        if (!missing.isEmpty()) {
            throw new ProductSnapshotMismatchException(missing);
        }

        CheckoutContextClientResponse ctx = fetchCheckoutContext(request.addressId());
        UserInfoClientResponse userInfo = ctx.user();
        AddressClientResponse address = ctx.address();

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .currency(DEFAULT_CURRENCY)
                .addressId(request.addressId())
                .correlationId(correlationId)
                .totalAmount(BigDecimal.ZERO)
                .identityNumber(request.identityNumber())
                .ip(ip)
                .buyerFirstName(userInfo.firstName())
                .buyerLastName(userInfo.lastName())
                .buyerEmail(userInfo.email() != null ? userInfo.email() : userEmail)
                .buyerPhone(userInfo.phone())
                .shippingContactName(address.contactName())
                .shippingAddress(address.fullAddress())
                .shippingCity(address.city())
                .shippingDistrict(address.district())
                .shippingCountry(address.country() != null ? address.country() : "Turkey")
                .shippingZipCode(address.zipCode())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;
        for (CartItemClientResponse cartItem : cart.items()) {
            ProductClientResponse product = productMap.get(cartItem.productId());
            OrderItem item = OrderItem.builder()
                    .productId(product.id())
                    .productName(product.name())
                    .unitPrice(product.price())
                    .quantity(cartItem.quantity())
                    .currency(product.currency() != null ? product.currency() : DEFAULT_CURRENCY)
                    .build();
            order.addItem(item);
            total = total.add(item.getSubtotal());
            if (product.currency() != null) {
                currency = product.currency();
            }
        }
        order.setTotalAmount(total);
        order.setCurrency(currency);

        Order saved = orderRepository.save(order);
        log.info("Order persisted: id={}, userId={}, totalAmount={} {}",
                saved.getId(), userId, saved.getTotalAmount(), saved.getCurrency());

        publishOrderCreated(saved, address, userInfo, correlationId);

        safeClearCart(saved.getId());

        return orderMapper.toResponse(saved);
    }

    @Transactional
    public void handleStockReserved(UUID orderId, String correlationId) {
        Order order = loadForUpdate(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Skipping STOCK_RESERVED — orderId={} already in state {}",
                    orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.STOCK_RESERVED);
        log.info("Order moved to STOCK_RESERVED: orderId={}, correlationId={}",
                orderId, correlationId);
    }

    @Transactional
    public void handleStockFailed(UUID orderId, String correlationId) {
        Order order = loadForUpdate(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Skipping STOCK_FAILED — orderId={} already in state {}",
                    orderId, order.getStatus());
            return;
        }

        cancelOrderInternal(order, CancelReason.STOCK_UNAVAILABLE, correlationId);
    }

    @Transactional
    public void handlePaymentCompleted(UUID orderId, String correlationId) {
        Order order = loadForUpdate(orderId);

        if (order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED) {
            log.info("Skipping PAYMENT_COMPLETED — orderId={} already in state {}",
                    orderId, order.getStatus());
            return;
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Received PAYMENT_COMPLETED but orderId={} is CANCELLED — ignoring", orderId);
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        publishOrderConfirmed(order, correlationId);
        log.info("Order CONFIRMED: orderId={}, correlationId={}", orderId, correlationId);
    }

    @Transactional
    public void handlePaymentFailed(UUID orderId, String correlationId) {
        Order order = loadForUpdate(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED) {
            log.info("Skipping PAYMENT_FAILED — orderId={} already in state {}",
                    orderId, order.getStatus());
            return;
        }

        cancelOrderInternal(order, CancelReason.PAYMENT_FAILED, correlationId);
    }

    @Transactional
    public void handleShipmentCreated(UUID orderId, String correlationId) {
        Order order = loadForUpdate(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            log.info("Skipping SHIPMENT_CREATED — orderId={} in state {}",
                    orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.SHIPPED);
        log.info("Order SHIPPED: orderId={}, correlationId={}", orderId, correlationId);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserIdAndIsActiveTrue(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus status = order.getStatus();
        boolean cancellable = status == OrderStatus.PENDING
                || status == OrderStatus.STOCK_RESERVED
                || status == OrderStatus.CONFIRMED;
        if (!cancellable) {
            throw new InvalidOrderStateException(status, "cancel");
        }

        String correlationId = MDC.get("correlationId");
        cancelOrderInternal(order, CancelReason.USER_CANCELLED, correlationId);
        return orderMapper.toResponse(order);
    }

    private void cancelOrderInternal(Order order, CancelReason reason, String correlationId) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        publishOrderCancelled(order, reason, correlationId);
        log.info("Order CANCELLED: orderId={}, reason={}, correlationId={}",
                order.getId(), reason, correlationId);
    }

    private Order loadForUpdate(UUID orderId) {
        return orderRepository.findByIdAndIsActiveTrue(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private CartClientResponse fetchCart() {
        ApiResponse<CartClientResponse> response = cartClient.getCart();
        return response != null ? response.data() : null;
    }

    private Map<UUID, ProductClientResponse> fetchProducts(List<UUID> ids) {
        ApiResponse<List<ProductClientResponse>> response = productClient.getProductsByIds(ids);
        List<ProductClientResponse> products = response != null && response.data() != null
                ? response.data()
                : List.of();
        return products.stream().collect(Collectors.toMap(ProductClientResponse::id, Function.identity()));
    }

    private CheckoutContextClientResponse fetchCheckoutContext(UUID addressId) {
        ApiResponse<CheckoutContextClientResponse> response = userClient.getCheckoutContext(addressId);
        if (response == null || response.data() == null) {
            throw new UserServiceUnavailableException();
        }
        return response.data();
    }

    private void safeClearCart(UUID orderId) {
        try {
            cartClient.clearCart();
            log.info("Cart cleared after order creation: orderId={}", orderId);
        } catch (Exception e) {
            log.warn("Cart cleanup failed (non-fatal, cart-service TTL will handle): orderId={}",
                    orderId, e);
        }
    }

    private void publishOrderCreated(Order order, AddressClientResponse address,
                                     UserInfoClientResponse userInfo, String correlationId) {
        List<OrderEventItem> items = orderMapper.toEventItems(order.getItems());

        BuyerInfo buyer = new BuyerInfo(
                order.getBuyerFirstName(),
                order.getBuyerLastName(),
                order.getBuyerEmail(),
                order.getBuyerPhone(),
                order.getIdentityNumber(),
                order.getIp()
        );

        ShippingAddress shippingAddress = new ShippingAddress(
                order.getShippingContactName(),
                order.getShippingAddress(),
                order.getShippingCity(),
                order.getShippingDistrict(),
                order.getShippingCountry(),
                order.getShippingZipCode(),
                address.phone()
        );

        OrderCreatedPayload payload = new OrderCreatedPayload(
                order.getId(), order.getUserId(), items, buyer, shippingAddress
        );
        publishOutbox(EventType.ORDER_CREATED, order.getId().toString(), payload, correlationId);
    }

    private void publishOrderConfirmed(Order order, String correlationId) {
        OrderConfirmedPayload payload = new OrderConfirmedPayload(order.getId(), order.getUserId());
        publishOutbox(EventType.ORDER_CONFIRMED, order.getId().toString(), payload, correlationId);
    }

    private void publishOrderCancelled(Order order, CancelReason reason, String correlationId) {
        OrderCancelledPayload payload = new OrderCancelledPayload(order.getId(), order.getUserId(), reason.name());
        publishOutbox(EventType.ORDER_CANCELLED, order.getId().toString(), payload, correlationId);
    }

    private void publishOutbox(EventType eventType, String aggregateId,
                               Object payload, String correlationId) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(AGGREGATE_TYPE)
                    .aggregateId(aggregateId)
                    .eventType(eventType.name())
                    .payload(objectMapper.writeValueAsString(payload))
                    .correlationId(correlationId)
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for eventType={}", eventType, e);
            throw new RuntimeException("Outbox serialization failed", e);
        }
    }
}
