package com.firefly.example.kotlin.test.dao

import com.firefly.example.kotlin.coffee.store.dao.InventoryDAO
import com.firefly.example.kotlin.test.TestBase
import com.firefly.example.kotlin.coffee.store.vo.InventoryOperator
import com.firefly.example.kotlin.coffee.store.vo.InventoryUpdate
import com.firefly.kotlin.ext.context.Context
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
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

        Unit
    }
}