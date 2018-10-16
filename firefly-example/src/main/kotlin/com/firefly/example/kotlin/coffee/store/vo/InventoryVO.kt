package com.firefly.example.kotlin.coffee.store.vo

import com.firefly.kotlin.ext.annotation.NoArg

/**
 * @author Pengtao Qiu
 */
@NoArg
data class InventoryUpdate(
    var productId: Long,
    var amount: Long
                          )

enum class InventoryOperator(val value: String) {
    ADD("+"), SUB("-")
}