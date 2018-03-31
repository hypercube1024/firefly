package com.firefly.example.kotlin.coffee.store.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.example.kotlin.coffee.store.dao.OrderDAO
import com.firefly.example.kotlin.coffee.store.model.Order
import com.firefly.kotlin.ext.db.AsyncTransactionalManager
import com.firefly.kotlin.ext.db.asyncInsertObjectBatch

/**
 * @author Pengtao Qiu
 */
@Component
class OrderDAOImpl : OrderDAO {

    @Inject
    private lateinit var db: AsyncTransactionalManager

    override suspend fun insertBatch(orders: List<Order>): List<Long> = db.execSQL {
        it.asyncInsertObjectBatch<Order, Long>(orders)
    }

}