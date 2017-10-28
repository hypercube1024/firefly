package com.firefly.example.kotlin.coffee.store.dao

import com.firefly.example.kotlin.coffee.store.model.Product
import com.firefly.example.kotlin.coffee.store.vo.Page
import com.firefly.example.kotlin.coffee.store.vo.ProductQuery

/**
 * @author Pengtao Qiu
 */
interface ProductDAO {

    suspend fun list(query: ProductQuery): Page<Product>

    suspend fun get(id: Long): Product

    suspend fun list(idList: List<Long>): List<Product>

    suspend fun insert(product: Product): Long

    suspend fun update(product: Product): Int

}