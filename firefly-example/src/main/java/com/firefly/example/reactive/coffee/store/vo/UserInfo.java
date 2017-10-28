package com.firefly.example.reactive.coffee.store.vo;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class UserInfo {
    private Long id;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return $.json.toJson(this);
    }
}
