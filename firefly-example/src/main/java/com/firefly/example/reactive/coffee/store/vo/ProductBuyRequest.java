package com.firefly.example.reactive.coffee.store.vo;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class ProductBuyRequest {

    private Long userId;
    private List<InventoryUpdate> products;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<InventoryUpdate> getProducts() {
        return products;
    }

    public void setProducts(List<InventoryUpdate> products) {
        this.products = products;
    }

}
