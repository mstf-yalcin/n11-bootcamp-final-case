package com.n11.bootcamp.order_service.mapper;

import com.n11.bootcamp.common_lib.event.order.OrderEventItem;
import com.n11.bootcamp.order_service.dto.response.OrderItemResponse;
import com.n11.bootcamp.order_service.dto.response.OrderResponse;
import com.n11.bootcamp.order_service.entity.Order;
import com.n11.bootcamp.order_service.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = OrderMapperHelper.class)
public interface OrderMapper {

    @Mapping(target = "subtotal", expression = "java(item.getSubtotal())")
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponses(List<OrderItem> items);

    @Mapping(target = "buyerFullName", expression = "java(OrderMapperHelper.joinName(order.getBuyerFirstName(), order.getBuyerLastName()))")
    OrderResponse toResponse(Order order);

    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    OrderEventItem toEventItem(OrderItem item);

    List<OrderEventItem> toEventItems(List<OrderItem> items);
}
