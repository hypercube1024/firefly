package com.firefly.example.kotlin.coffee.store.vo

import com.firefly.example.kotlin.coffee.store.model.Product
import com.firefly.kotlin.ext.annotation.NoArg

/**
 * @author Pengtao Qiu
 */
@NoArg
data class ProductQuery(
    var searchKey: String? = null,
    var status: Int? = null,
    var type: Int? = null,
    var pageNumber: Int = 1,
    var pageSize: Int = 20
                       )

@NoArg
data class ProductBuyRequest(
    var userId: Long?,
    var products: List<InventoryUpdate>
                            )

enum class ProductStatus(
    val value: Int,
    val description: String
                        ) {
    ENABLE(1, "enable"), DISABLE(2, "disable")
}

enum class ProductType(
    val value: Int,
    val description: String
                      ) {
    COFFEE(1, "coffee"), DESSERT(2, "dessert")
}

@NoArg
data class MainPage(
    var userInfo: UserInfo?,
    var products: Page<Product>,
    var type: Int?,
    var searchKey: String?
                   )