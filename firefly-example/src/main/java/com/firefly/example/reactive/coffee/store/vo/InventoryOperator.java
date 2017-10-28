package com.firefly.example.reactive.coffee.store.vo;

/**
 * @author Pengtao Qiu
 */
public enum InventoryOperator {
    ADD("+"), SUB("-");

    private final String value;

    InventoryOperator(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
