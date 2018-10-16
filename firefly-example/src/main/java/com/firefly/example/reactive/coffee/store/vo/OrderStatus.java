package com.firefly.example.reactive.coffee.store.vo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public enum OrderStatus {

    UNPAID(1, "unpaid"),
    PURCHASED(2, "purchased"),
    SHIPPED(3, "shipped"),
    FINISHED(4, "finished"),
    CANCELED(5, "canceled"),
    REFUND(6, "refund");

    private final int value;
    private final String description;

    static class Holder {
        static final Map<Integer, OrderStatus> map = new HashMap<>();
    }

    OrderStatus(int value, String description) {
        this.value = value;
        this.description = description;
        Holder.map.put(value, this);
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<OrderStatus> from(int value) {
        return Optional.ofNullable(Holder.map.get(value));
    }
}
