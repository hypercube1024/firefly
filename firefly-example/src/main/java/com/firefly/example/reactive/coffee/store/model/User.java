package com.firefly.example.reactive.coffee.store.model;

import com.firefly.$;
import com.firefly.db.annotation.Column;
import com.firefly.db.annotation.Id;
import com.firefly.db.annotation.Table;

import java.util.Date;

/**
 * @author Pengtao Qiu
 */
@Table(value = "user", catalog = "coffee_store")
public class User {

    @Id("id")
    private Long id;

    @Column("name")
    private String name;

    @Column("password")
    private String password;

    @Column("create_time")
    private Date createTime;

    @Column("update_time")
    private Date updateTime;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return $.json.toJson(this);
    }
}
