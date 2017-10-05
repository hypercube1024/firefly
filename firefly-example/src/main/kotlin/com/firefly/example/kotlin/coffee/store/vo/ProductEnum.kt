package com.firefly.example.kotlin.coffee.store.vo

/**
 * @author Pengtao Qiu
 */
enum class ProductStatus(val value: Int,
                         val description: String) {
    ENABLE(1, "enable"), DISABLE(2, "disable")
}

enum class ProductType(val value: Int,
                       val description: String) {
    COFFEE(1, "coffee"), DESSERT(2, "dessert")
}