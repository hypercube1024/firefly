package com.firefly.example.reactive.coffee.store.model;

import com.firefly.db.annotation.Column;
import com.firefly.db.annotation.Id;
import com.firefly.db.annotation.Table;

import java.util.Date;

/**
 * @author Pengtao Qiu
 */
@Table(value = "order", catalog = "coffee_store")
public class Order {

    @Id("id")
    private Long id;

    @Column("status")
    private Integer status;

    @Column("amount")
    private Long amount;

    @Column("price")
    private Double price;

    @Column("total_price")
    private Double totalPrice;

    @Column("product_id")
    private Long productId;

    @Column("user_id")
    private Long userId;

    @Column("description")
    private String description;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

}
