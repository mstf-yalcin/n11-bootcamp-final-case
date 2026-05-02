package com.n11.bootcamp.order_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.common_lib.event.order.CancelReason;
import com.n11.bootcamp.common_lib.event.order.OrderStatus;
import com.n11.bootcamp.order_service.client.CartClient;
import com.n11.bootcamp.order_service.client.ProductClient;
import com.n11.bootcamp.order_service.client.UserClient;
import com.n11.bootcamp.order_service.client.dto.AddressClientResponse;
import com.n11.bootcamp.order_service.client.dto.CartClientResponse;
import com.n11.bootcamp.order_service.client.dto.CartItemClientResponse;
import com.n11.bootcamp.order_service.client.dto.CheckoutContextClientResponse;
import com.n11.bootcamp.order_service.client.dto.ProductClientResponse;
import com.n11.bootcamp.order_service.client.dto.UserInfoClientResponse;
import com.n11.bootcamp.order_service.dto.request.CreateOrderRequest;
import com.n11.bootcamp.order_service.entity.Order;
import com.n11.bootcamp.order_service.entity.OutboxEvent;
import com.n11.bootcamp.order_service.exception.EmptyCartException;
import com.n11.bootcamp.order_service.exception.InvalidOrderStateException;
import com.n11.bootcamp.order_service.mapper.OrderMapper;
import com.n11.bootcamp.order_service.repository.OrderRepository;
import com.n11.bootcamp.order_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private OrderMapper orderMapper;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private ProductClient productClient;
    @Mock
    private CartClient cartClient;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderService orderService;

    private static final String TEST_TC = "11111111111";
    private static final String TEST_IP = "127.0.0.1";
    private static final String TEST_EMAIL = "buyer@example.com";

    private CheckoutContextClientResponse stubCheckoutContext(UUID userId) {
        return new CheckoutContextClientResponse(
                new UserInfoClientResponse(userId, TEST_EMAIL, "John", "Doe", "+905350000000"),
                new AddressClientResponse(UUID.randomUUID(), "Ev", "John Doe",
                        "Test Mah. Test Sok. No:1", "Istanbul", "Kadıköy",
                        "Turkey", "34000", "+905350000000", true)
        );
    }

    @Test
    void testCreateOrder_when_cartHasItems_persistsOrderAndPublishesOrderCreated() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        CartItemClientResponse cartItem = new CartItemClientResponse(
                productId, "Laptop", "img.jpg", new BigDecimal("100.00"), "TRY", 2, new BigDecimal("200.00"));
        CartClientResponse cart = new CartClientResponse(
                userId, List.of(cartItem), new BigDecimal("200.00"), "TRY", Instant.now());

        ProductClientResponse product = new ProductClientResponse(
                productId, "Laptop", new BigDecimal("100.00"), "TRY", "img.jpg");

        when(cartClient.getCart()).thenReturn(ApiResponse.success(cart, "ok"));
        when(productClient.getProductsByIds(anyList())).thenReturn(ApiResponse.success(List.of(product), "ok"));
        when(userClient.getCheckoutContext(addressId)).thenReturn(ApiResponse.success(stubCheckoutContext(userId), "ok"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            return o;
        });
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartClient.clearCart()).thenReturn(ApiResponse.success("cleared"));
        when(orderMapper.toEventItems(anyList())).thenReturn(List.of());
        when(orderMapper.toResponse(any(Order.class))).thenReturn(null);

        orderService.createOrder(userId, TEST_EMAIL, TEST_IP, new CreateOrderRequest(addressId, TEST_TC));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getUserId()).isEqualTo(userId);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(savedOrder.getItems()).hasSize(1);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(outboxCaptor.getValue().getAggregateType()).isEqualTo("order");

        verify(cartClient).clearCart();
    }

    @Test
    void testCreateOrder_when_cartEmpty_throwsEmptyCartException() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        CartClientResponse emptyCart = new CartClientResponse(
                userId, List.of(), BigDecimal.ZERO, "TRY", Instant.now());
        when(cartClient.getCart()).thenReturn(ApiResponse.success(emptyCart, "ok"));

        assertThatThrownBy(() -> orderService.createOrder(userId, TEST_EMAIL, TEST_IP, new CreateOrderRequest(addressId, TEST_TC)))
                .isInstanceOf(EmptyCartException.class);

        verify(orderRepository, never()).save(any(Order.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void testHandleStockFailed_when_pending_cancelsAndPublishesOrderCancelled() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .userId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("200.00"))
                .currency("TRY")
                .addressId(UUID.randomUUID())
                .build();
        order.setId(orderId);

        when(orderRepository.findByIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.handleStockFailed(orderId, "corr-3");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo(CancelReason.STOCK_UNAVAILABLE);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    void testHandlePaymentCompleted_when_stockReserved_confirmsAndPublishesOrderConfirmed() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .userId(UUID.randomUUID())
                .status(OrderStatus.STOCK_RESERVED)
                .totalAmount(new BigDecimal("200.00"))
                .currency("TRY")
                .addressId(UUID.randomUUID())
                .build();
        order.setId(orderId);

        when(orderRepository.findByIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.handlePaymentCompleted(orderId, "corr-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_CONFIRMED");
        assertThat(captor.getValue().getAggregateType()).isEqualTo("order");
    }

    @Test
    void testHandlePaymentFailed_when_stockReserved_cancelsAndPublishesOrderCancelled() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .userId(UUID.randomUUID())
                .status(OrderStatus.STOCK_RESERVED)
                .totalAmount(new BigDecimal("200.00"))
                .currency("TRY")
                .addressId(UUID.randomUUID())
                .build();
        order.setId(orderId);

        when(orderRepository.findByIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.handlePaymentFailed(orderId, "corr-2");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo(CancelReason.PAYMENT_FAILED);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    void testCancelOrder_when_pendingOrStockReserved_cancelsAndPublishesOrderCancelled() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.STOCK_RESERVED)
                .totalAmount(new BigDecimal("200.00"))
                .currency("TRY")
                .addressId(UUID.randomUUID())
                .build();
        order.setId(orderId);

        when(orderRepository.findByIdAndUserIdAndIsActiveTrue(orderId, userId)).thenReturn(Optional.of(order));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(null);

        orderService.cancelOrder(orderId, userId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo(CancelReason.USER_CANCELLED);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    void testCancelOrder_when_confirmed_cancelsAndPublishesOrderCancelled() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("200.00"))
                .currency("TRY")
                .addressId(UUID.randomUUID())
                .build();
        order.setId(orderId);

        when(orderRepository.findByIdAndUserIdAndIsActiveTrue(orderId, userId)).thenReturn(Optional.of(order));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(null);

        orderService.cancelOrder(orderId, userId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo(CancelReason.USER_CANCELLED);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    void testCancelOrder_when_shipped_throwsInvalidOrderStateException() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.SHIPPED)
                .totalAmount(new BigDecimal("200.00"))
                .currency("TRY")
                .addressId(UUID.randomUUID())
                .build();

        when(orderRepository.findByIdAndUserIdAndIsActiveTrue(orderId, userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void testCancelOrder_when_paymentProcessing_throwsInvalidOrderStateException() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PAYMENT_PROCESSING)
                .totalAmount(new BigDecimal("200.00"))
                .currency("TRY")
                .addressId(UUID.randomUUID())
                .build();

        when(orderRepository.findByIdAndUserIdAndIsActiveTrue(orderId, userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(InvalidOrderStateException.class);
    }

}
