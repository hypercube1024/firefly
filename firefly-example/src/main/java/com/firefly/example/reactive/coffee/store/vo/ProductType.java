package com.firefly.example.reactive.coffee.store.vo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public enum ProductType {

    COFFEE(1, "coffee"), DESSERT(2, "dessert");

    private final int value;
    private final String description;

    static class Holder {
        static final Map<Integer, ProductType> map = new HashMap<>();
    }

    ProductType(int value, String description) {
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

    public static Optional<ProductType> from(int value) {
        return Optional.ofNullable(Holder.map.get(value));
    }
}
