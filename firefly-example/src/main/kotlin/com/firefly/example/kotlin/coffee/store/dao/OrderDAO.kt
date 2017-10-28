package com.firefly.example.kotlin.coffee.store.dao

import com.firefly.example.kotlin.coffee.store.model.Order

/**
 * @author Pengtao Qiu
 */
interface OrderDAO {

    suspend fun insertBatch(orders: List<Order>): List<Long>

}