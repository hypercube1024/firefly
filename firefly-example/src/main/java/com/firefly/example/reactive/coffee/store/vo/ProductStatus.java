package com.firefly.example.reactive.coffee.store.vo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public enum ProductStatus {

    ENABLE(1, "enable"), DISABLE(2, "disable");

    private final int value;
    private final String description;

    static class Holder {
        static final Map<Integer, ProductStatus> map = new HashMap<>();
    }

    ProductStatus(int value, String description) {
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

    public static Optional<ProductStatus> from(int value) {
        return Optional.ofNullable(Holder.map.get(value));
    }
}
