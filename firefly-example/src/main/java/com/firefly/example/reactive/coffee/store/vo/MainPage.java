package com.firefly.example.reactive.coffee.store.vo;

import com.firefly.example.reactive.coffee.store.model.Product;

/**
 * @author Pengtao Qiu
 */
public class MainPage {

    private UserInfo userInfo;
    private Page<Product> products;
    private Integer type;
    private String searchKey;

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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }
}
