package com.firefly.example.kotlin.coffee.store.model

import com.firefly.db.annotation.Column
import com.firefly.db.annotation.Id
import com.firefly.db.annotation.Table
import java.util.*

/**
 * @author Pengtao Qiu
 */
@Table(value = "inventory", catalog = "coffee_store")
data class Inventory(@Id("id") var id: Long,
                     @Column("amount") var amount: Long,
                     @Column("product_id") var productId: Long,
                     @Column("create_time") var createTime: Date,
                     @Column("update_time") var updateTime: Date)