package com.firefly.example.kotlin.coffee.store.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.example.kotlin.coffee.store.dao.ProductDAO
import com.firefly.example.kotlin.coffee.store.model.Product
import com.firefly.example.kotlin.coffee.store.utils.DBUtils.Companion.toWildcard
import com.firefly.example.kotlin.coffee.store.vo.Page
import com.firefly.example.kotlin.coffee.store.vo.ProductQuery
import com.firefly.kotlin.ext.db.*
import com.firefly.utils.CollectionUtils
import com.firefly.utils.StringUtils
import java.util.*

/**
 * @author Pengtao Qiu
 */
@Component
class ProductDAOImpl : ProductDAO {

    @Inject
    private lateinit var db: AsyncTransactionalManager

    override suspend fun list(query: ProductQuery): Page<Product> {
        val params = ArrayList<Any>()
        val sql = StringBuilder("select p.*, inventory.amount from `coffee_store`.`product` p ")
        sql.append("inner join `coffee_store`.`inventory` inventory on inventory.product_id = p.id where 1 = 1 ")

        Optional.ofNullable(query.searchKey)
            .filter(StringUtils::hasText)
            .ifPresent { key ->
                sql.append(" and p.`name` like ?")
                params.add(key + "%")
            }

        Optional.ofNullable(query.status)
            .filter { status -> status > 0 }
            .ifPresent { status ->
                sql.append(" and p.`status` = ?")
                params.add(status)
            }

        Optional.ofNullable(query.type)
            .filter { type -> type > 0 }
            .ifPresent { type ->
                sql.append(" and p.`type` = ?")
                params.add(type)
            }

        sql.append(" order by id desc ").append(Page.getPageSQLWithoutCount(query.pageNumber, query.pageSize))
        return db.execSQL {
            Page(
                it.asyncQueryForList<Product>(sql.toString(), *params.toTypedArray()).toMutableList(),
                query.pageNumber,
                query.pageSize
                )
        }
    }

    suspend override fun get(id: Long): Product = db.execSQL { it.asyncQueryById<Product>(id) }

    suspend override fun list(idList: List<Long>): List<Product> {
        if (CollectionUtils.isEmpty(idList)) {
            return listOf()
        }
        val sql = "select * from `coffee_store`.`product` where id in ( ${toWildcard(idList)} )"
        return db.execSQL { it.asyncQueryForList<Product>(sql, *idList.toTypedArray()) }
    }

    suspend override fun insert(product: Product): Long = db.execSQL { it.asyncInsertObject<Product, Long>(product) }

    suspend override fun update(product: Product): Int = db.execSQL { it.asyncUpdateObject(product) }

}