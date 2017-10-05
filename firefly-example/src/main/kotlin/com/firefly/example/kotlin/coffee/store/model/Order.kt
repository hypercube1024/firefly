package com.firefly.example.kotlin.coffee.store.model

import com.firefly.db.annotation.Column
import com.firefly.db.annotation.Id
import com.firefly.db.annotation.Table
import java.util.*

/**
 * @author Pengtao Qiu
 */
@Table(value = "order", catalog = "coffee_store")
data class Order(@Id("id") var id: Long,
                 @Column("status") var status: Int,
                 @Column("amount") var amount: Long,
                 @Column("price") var price: Double,
                 @Column("total_price") var totalPrice: Double,
                 @Column("product_id") var productId: Long,
                 @Column("user_id") var userId: Long,
                 @Column("description") var description: String,
                 @Column("create_time") var createTime: Date,
                 @Column("update_time") var updateTime: Date) {

    override fun equals(other: Any?): Boolean = if (other is Order) Objects.equals(id, other.id) else false

    override fun hashCode(): Int = Objects.hashCode(id)
}