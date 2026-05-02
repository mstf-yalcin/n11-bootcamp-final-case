package com.n11.bootcamp.order_service.mapper;

public final class OrderMapperHelper {

    private OrderMapperHelper() {}

    public static String joinName(String first, String last) {
        boolean hasFirst = first != null && !first.isBlank();
        boolean hasLast = last != null && !last.isBlank();
        if (hasFirst && hasLast) return first + " " + last;
        if (hasFirst) return first;
        if (hasLast) return last;
        return null;
    }
}
