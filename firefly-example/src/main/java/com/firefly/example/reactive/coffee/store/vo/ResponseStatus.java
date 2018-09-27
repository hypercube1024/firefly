package com.firefly.example.reactive.coffee.store.vo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public enum ResponseStatus {

    OK(1, "ok"),
    ARGUMENT_ERROR(2, "argument error"),
    SERVER_ERROR(3, "server error");

    private final int value;
    private final String description;

    static class Holder {
        static final Map<Integer, ResponseStatus> map = new HashMap<>();
    }

    ResponseStatus(int value, String description) {
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

    public static Optional<ResponseStatus> from(int value) {
        return Optional.ofNullable(Holder.map.get(value));
    }
}
