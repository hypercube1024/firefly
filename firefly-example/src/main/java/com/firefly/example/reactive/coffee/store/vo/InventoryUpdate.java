package com.firefly.example.reactive.coffee.store.vo;

/**
 * @author Pengtao Qiu
 */
public class InventoryUpdate {
    private Long productId;
    private Long amount;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}
