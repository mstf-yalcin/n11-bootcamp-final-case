package com.n11.bootcamp.common_lib.event;

public enum EventType {

    // Order events
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_CANCELLED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,

    // Stock events
    STOCK_RESERVED,
    STOCK_FAILED,

    // Payment events
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,

    // Shipment events
    SHIPMENT_CREATED,
    SHIPMENT_UPDATED
}
