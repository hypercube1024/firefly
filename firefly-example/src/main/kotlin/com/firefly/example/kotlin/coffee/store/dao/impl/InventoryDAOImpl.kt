package com.firefly.example.kotlin.coffee.store.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.example.kotlin.coffee.store.dao.InventoryDAO
import com.firefly.example.kotlin.coffee.store.model.Inventory
import com.firefly.example.kotlin.coffee.store.utils.DBUtils.Companion.toWildcard
import com.firefly.example.kotlin.coffee.store.vo.InventoryOperator
import com.firefly.example.kotlin.coffee.store.vo.InventoryUpdate
import com.firefly.kotlin.ext.db.AsyncTransactionalManager
import com.firefly.kotlin.ext.db.asyncQueryForList
import com.firefly.utils.CollectionUtils
import kotlinx.coroutines.experimental.future.await
import java.util.*
import java.util.stream.Collectors

/**
 * @author Pengtao Qiu
 */
@Component
class InventoryDAOImpl : InventoryDAO {

    @Inject
    lateinit var db: AsyncTransactionalManager

    suspend override fun updateBatch(list: List<InventoryUpdate>, operator: InventoryOperator): IntArray = db.execSQL {
        if (CollectionUtils.isEmpty(list)) {
            throw IllegalArgumentException("The inventory update request must be not empty")
        }

        var sql = "update `coffee_store`.`inventory` set `amount` = `amount` ${operator.value} ?  where `product_id` = ? "
        if (operator == InventoryOperator.SUB) {
            sql += " and `amount` >= ? "
        }

        it.executeBatch(sql, list.parallelStream().map { u ->
            val p = ArrayList<Any>()
            p.add(u.amount)
            p.add(u.productId)
            if (operator == InventoryOperator.SUB) {
                p.add(u.amount)
            }
            p.toTypedArray()
        }.collect(Collectors.toList<Array<Any>>()).toTypedArray()).await()
    }

    suspend override fun listByProductId(productIdList: List<Long>): List<Inventory> {
        if (CollectionUtils.isEmpty(productIdList)) {
            return listOf()
        }

        return db.execSQL {
            val sql = "select * from `coffee_store`.`inventory` where `product_id` in ( ${toWildcard(productIdList)} )"
            it.asyncQueryForList<Inventory>(sql, *productIdList.toTypedArray())
        }
    }

}