package com.firefly.example.reactive.coffee.store.model;

import com.firefly.$;
import com.firefly.db.annotation.Column;
import com.firefly.db.annotation.Id;
import com.firefly.db.annotation.Table;
import com.firefly.example.reactive.coffee.store.vo.ProductStatus;
import com.firefly.example.reactive.coffee.store.vo.ProductType;

import java.util.Date;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
@Table(value = "product", catalog = "coffee_store")
public class Product {

    @Id("id")
    private Long id;

    @Column("type")
    private Integer type;

    @Column("name")
    private String name;

    @Column("price")
    private Double price;

    @Column("status")
    private Integer status;

    @Column("description")
    private String description;

    @Column("create_time")
    private Date createTime;

    @Column("update_time")
    private Date updateTime;

    private Long amount;

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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getStatusDesc() {
        return Optional.ofNullable(status)
                       .flatMap(ProductStatus::from)
                       .map(ProductStatus::getDescription)
                       .orElse("--");
    }

    public String getTypeDesc() {
        return Optional.ofNullable(type)
                       .flatMap(ProductType::from)
                       .map(ProductType::getDescription)
                       .orElse("--");
    }

    @Override
    public String toString() {
        return $.json.toJson(this);
    }
}
