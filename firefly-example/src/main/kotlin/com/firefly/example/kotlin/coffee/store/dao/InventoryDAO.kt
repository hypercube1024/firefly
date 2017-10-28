package com.firefly.example.kotlin.coffee.store.dao

import com.firefly.example.kotlin.coffee.store.model.Inventory
import com.firefly.example.kotlin.coffee.store.vo.InventoryOperator
import com.firefly.example.kotlin.coffee.store.vo.InventoryUpdate

/**
 * @author Pengtao Qiu
 */
interface InventoryDAO {

    suspend fun updateBatch(list: List<InventoryUpdate>, operator: InventoryOperator): IntArray

    suspend fun listByProductId(productIdList: List<Long>): List<Inventory>

}