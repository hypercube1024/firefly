package com.firefly.example.kotlin.coffee.store.model

import com.firefly.db.annotation.Column
import com.firefly.db.annotation.Id
import com.firefly.db.annotation.Table
import java.util.*

/**
 * @author Pengtao Qiu
 */
@Table(value = "product", catalog = "coffee_store")
data class Product(@Id("id") var id: Long?,
                   @Column("type") var type: Int,
                   @Column("name") var name: String,
                   @Column("price") var price: Double,
                   @Column("status") var status: Int,
                   @Column("description") var description: String,
                   @Column("create_time") var createTime: Date?,
                   @Column("update_time") var updateTime: Date?,
                   var amount: Long) {

    override fun equals(other: Any?): Boolean = if (other is Product) Objects.equals(id, other.id) else false

    override fun hashCode(): Int = Objects.hashCode(id)
}