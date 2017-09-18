package com.firefly.example.reactive.coffee.store.vo;

import com.firefly.example.reactive.coffee.store.model.Product;

/**
 * @author Pengtao Qiu
 */
public class MainPage {

    private String userName;
    private Page<Product> products;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Page<Product> getProducts() {
        return products;
    }

    public void setProducts(Page<Product> products) {
        this.products = products;
    }
}
