package com.firefly.kotlin.ext.db

import com.firefly.db.SQLConnection
import com.firefly.db.SQLResultSet
import com.firefly.kotlin.ext.log.KtLogger
import com.firefly.utils.function.Func1
import kotlinx.coroutines.experimental.future.await

/**
 * @author Pengtao Qiu
 */

val sysLogger = KtLogger.getLogger("firefly-system")


// query for single column
suspend fun <T> SQLConnection.asyncQueryForSingleColumn(sql: String, vararg params: Any): T =
        this.queryForSingleColumn<T>(sql, *params).await()

suspend fun <T> SQLConnection.asyncNamedQueryForSingleColumn(sql: String, paramMap: Map<String, Any>): T =
        this.namedQueryForSingleColumn<T>(sql, paramMap).await()

suspend fun <T> SQLConnection.asyncNamedQueryForSingleColumn(sql: String, paramObject: Any): T =
        this.namedQueryForSingleColumn<T>(sql, paramObject).await()


// query for object
suspend inline fun <reified T> SQLConnection.asyncQueryForObject(sql: String, vararg params: Any): T =
        this.queryForObject<T>(sql, T::class.java, *params).await()

suspend inline fun <reified T> SQLConnection.asyncNamedQueryForObject(sql: String, paramMap: Map<String, Any>): T =
        this.namedQueryForObject(sql, T::class.java, paramMap).await()

suspend inline fun <reified T> SQLConnection.asyncNamedQueryForObject(sql: String, paramObject: Any): T =
        this.namedQueryForObject(sql, T::class.java, paramObject).await()


// query by id
suspend inline fun <reified T> SQLConnection.asyncQueryById(id: Any): T = this.queryById(id, T::class.java).await()


// query for bean map
suspend inline fun <K, reified V> SQLConnection.asyncQueryForBeanMap(sql: String, vararg params: Any): Map<K, V> =
        this.queryForBeanMap<K, V>(sql, V::class.java, *params).await() ?: mapOf()

suspend inline fun <K, reified V> SQLConnection.asyncNamedQueryForBeanMap(sql: String, paramMap: Map<String, Any>): Map<K, V> =
        this.namedQueryForBeanMap<K, V>(sql, V::class.java, paramMap).await() ?: mapOf()

suspend inline fun <K, reified V> SQLConnection.asyncNamedQueryForBeanMap(sql: String, paramObject: Any): Map<K, V> =
        this.namedQueryForBeanMap<K, V>(sql, V::class.java, paramObject).await() ?: mapOf()


// query for list
suspend inline fun <reified T> SQLConnection.asyncQueryForList(sql: String, vararg params: Any): List<T> =
        this.queryForList<T>(sql, T::class.java, *params).await() ?: listOf()

suspend inline fun <reified T> SQLConnection.asyncNamedQueryForList(sql: String, paramMap: Map<String, Any>): List<T> =
        this.namedQueryForList(sql, T::class.java, paramMap).await() ?: listOf()

suspend inline fun <reified T> SQLConnection.asyncNamedQueryForList(sql: String, paramObject: Any): List<T> =
        this.namedQueryForList(sql, T::class.java, paramObject).await() ?: listOf()


// query
suspend fun <T> SQLConnection.asyncQuery(sql: String, rsh: (rs: SQLResultSet) -> T, vararg params: Any): T =
        this.query(sql, Func1<SQLResultSet, T> { rsh.invoke(it) }, *params).await()

suspend fun <T> SQLConnection.asyncNamedQuery(sql: String, rsh: (rs: SQLResultSet) -> T, paramMap: Map<String, Any>): T =
        this.namedQuery(sql, { rsh.invoke(it) }, paramMap).await()

suspend fun <T> SQLConnection.asyncNamedQuery(sql: String, rsh: (rs: SQLResultSet) -> T, paramObject: Any): T =
        this.namedQuery(sql, { rsh.invoke(it) }, paramObject).await()


// update
suspend fun SQLConnection.asyncUpdate(sql: String, vararg params: Any): Int {
    return this.update(sql, *params).await() ?: 0
}

suspend fun SQLConnection.asyncNamedUpdate(sql: String, paramMap: Map<String, Any>): Int =
        this.namedUpdate(sql, paramMap).await() ?: 0

suspend fun SQLConnection.asyncNamedUpdate(sql: String, paramObject: Any): Int =
        this.namedUpdate(sql, paramObject).await() ?: 0


// update mapped object
suspend fun <T> SQLConnection.asyncUpdateObject(obj: T): Int {
    return this.updateObject(obj).await() ?: 0
}


// insert
suspend fun <R> SQLConnection.asyncInsert(sql: String, vararg params: Any): R = this.insert<R>(sql, *params).await()

suspend fun <R> SQLConnection.asyncInsert(sql: String, paramMap: Map<String, Any>): R = this.namedInsert<R>(sql, paramMap).await()

suspend fun <R> SQLConnection.asyncInsert(sql: String, paramObject: Any): R = this.namedInsert<R>(sql, paramObject).await()

suspend fun <T, R> SQLConnection.asyncInsertObject(obj: T): R = this.insertObject<T, R>(obj).await()

suspend inline fun <reified T, R> SQLConnection.asyncInsertObjectBatch(list: List<T>, rsh: Func1<SQLResultSet, R?>): R =
        this.insertObjectBatch<T, R>(list, T::class.java, rsh).await()

suspend inline fun <reified T, R> SQLConnection.asyncInsertObjectBatch(list: List<T>): List<R> =
        this.insertObjectBatch<T, R>(list, T::class.java).await() ?: listOf()


// delete
suspend inline fun <reified T> SQLConnection.asyncDeleteById(id: Any): Int =
        this.deleteById(id, T::class.java).await() ?: 0


suspend fun <T> SQLConnection.execSQL(handler: suspend (conn: SQLConnection) -> T): T {
    val isNew = beginTransaction().await()
    return try {
        val ret = handler.invoke(this)
        if (isNew) {
            commitAndEndTransaction().await()
        }
        ret
    } catch (e: Exception) {
        sysLogger.error("execute SQL exception", e)
        (if (isNew) rollbackAndEndTransaction() else rollback()).await()
        throw e
    }
}