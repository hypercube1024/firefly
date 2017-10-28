package com.firefly.example.reactive.coffee.store.model;

import com.firefly.db.annotation.Column;
import com.firefly.db.annotation.Id;
import com.firefly.db.annotation.Table;

import java.util.Date;

/**
 * @author Pengtao Qiu
 */
@Table(value = "inventory", catalog = "coffee_store")
public class Inventory {

    @Id("id")
    private Long id;

    @Column("amount")
    private Long amount;

    @Column("product_id")
    private Long productId;

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

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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
