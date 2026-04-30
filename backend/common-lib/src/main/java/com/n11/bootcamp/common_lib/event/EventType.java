package com.n11.bootcamp.common_lib.event;

public enum EventType {

    // Order events
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_CANCELLED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,

    // Inventory events
    INVENTORY_RESERVED,
    INVENTORY_FAILED,

    // Payment events
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,

    // Shipment events
    SHIPMENT_CREATED,
    SHIPMENT_UPDATED
}
