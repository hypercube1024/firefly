package com.firefly.example.reactive.coffee.store.vo;

import com.firefly.$;

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



    public static void main(String[] args) {
        String json = "{\"userId\":0,\"products\":[{\"productId\":\"7\",\"amount\":\"4\"},{\"productId\":\"6\",\"amount\":\"1\"},{\"productId\":\"5\",\"amount\":\"1\"},{\"productId\":\"4\",\"amount\":\"1\"},{\"productId\":\"3\",\"amount\":\"1\"}]}";
        ProductBuyRequest request = $.json.parse(json, ProductBuyRequest.class);
        System.out.println(request.getProducts().size());
    }
}
