package com.firefly.example.reactive.coffee.store.vo;

import com.firefly.example.reactive.coffee.store.model.Product;

/**
 * @author Pengtao Qiu
 */
public class MainPage {

    private UserInfo userInfo;
    private Page<Product> products;

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public Page<Product> getProducts() {
        return products;
    }

    public void setProducts(Page<Product> products) {
        this.products = products;
    }
}
