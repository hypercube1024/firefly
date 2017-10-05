package com.firefly.example.kotlin.test.dao

import com.firefly.example.kotlin.coffee.store.dao.InventoryDAO
import com.firefly.example.kotlin.coffee.store.model.Inventory
import com.firefly.example.kotlin.coffee.store.vo.InventoryOperator
import com.firefly.example.kotlin.coffee.store.vo.InventoryUpdate
import com.firefly.example.kotlin.test.TestBase
import com.firefly.kotlin.ext.context.Context
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import java.util.stream.Collectors
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestInventoryDAO : TestBase() {

    private val inventoryDAO = Context.getBean<InventoryDAO>()

    @Test
    fun test(): Unit = runBlocking {
        val ret = inventoryDAO.updateBatch(
                listOf(InventoryUpdate(4L, 10L),
                       InventoryUpdate(5L, 20L)),
                InventoryOperator.SUB)
        assertEquals(2, ret.size)
        ret.forEach { assertEquals(it, 1) }

        val map = inventoryDAO.listByProductId(listOf(4L, 5L))
                .parallelStream().collect(Collectors.toMap(Inventory::productId, { v -> v }))
        assertEquals(67L, map[4L]?.amount)
        assertEquals(80L, map[5L]?.amount)

        val ret2 = inventoryDAO.updateBatch(
                listOf(InventoryUpdate(4L, 90L),
                       InventoryUpdate(5L, 20L)),
                InventoryOperator.SUB)
        assertEquals(2, ret2.size)
        assertEquals(0, ret2[0])
        assertEquals(1, ret2[1])

        val map2 = inventoryDAO.listByProductId(listOf(4L, 5L))
                .parallelStream().collect(Collectors.toMap(Inventory::productId, { v -> v }))
        assertEquals(67L, map2[4L]?.amount)
        assertEquals(60L, map2[5L]?.amount)
        Unit
    }
}