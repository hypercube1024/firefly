package com.firefly.kotlin.ext.db

import com.firefly.db.SQLConnection
import com.firefly.db.SQLResultSet
import com.firefly.kotlin.ext.log.Log
import com.firefly.utils.exception.CommonRuntimeException
import com.firefly.utils.function.Func1
import kotlinx.coroutines.experimental.future.await

/**
 * @author Pengtao Qiu
 */

val sysLogger = Log.getLogger("firefly-system")

suspend fun <T> SQLConnection.asyncQueryForSingleColumn(sql: String, vararg params: Any): T {
    return this.queryForSingleColumn<T>(sql, *params).await()
}

suspend inline fun <reified T> SQLConnection.asyncQueryForObject(sql: String, vararg params: Any): T {
    return this.queryForObject<T>(sql, T::class.java, *params).await()
}

suspend inline fun <reified T> SQLConnection.asyncQueryById(id: Any): T {
    return this.queryById(id, T::class.java).await()
}

suspend inline fun <K, reified V> SQLConnection.asyncQueryForBeanMap(sql: String, vararg params: Any): Map<K, V> {
    return this.queryForBeanMap<K, V>(sql, V::class.java, *params).await() ?: mapOf()
}

suspend inline fun <reified T> SQLConnection.asyncQueryForList(sql: String, vararg params: Any): List<T> {
    return this.queryForList<T>(sql, T::class.java, *params).await() ?: listOf()
}

suspend fun <T> SQLConnection.asyncQuery(sql: String, rsh: (rs: SQLResultSet) -> T, vararg params: Any): T {
    return this.query(sql, Func1<SQLResultSet, T> { rsh.invoke(it) }, *params).await()
}

suspend fun SQLConnection.asyncUpdate(sql: String, vararg params: Any): Int {
    return this.update(sql, *params).await() ?: 0
}

suspend fun <T> SQLConnection.asyncUpdateObject(obj: T): Int {
    return this.updateObject(obj).await() ?: 0
}

suspend fun <R> SQLConnection.asyncInsert(sql: String, vararg params: Any): R {
    return this.insert<R>(sql, *params).await()
}

suspend fun <T, R> SQLConnection.asyncInsertObject(obj: T): R {
    return this.insertObject<T, R>(obj).await()
}

suspend inline fun <reified T, R> SQLConnection.asyncInsertObjectBatch(list: List<T>, rsh: Func1<SQLResultSet, R?>): R {
    return this.insertObjectBatch<T, R>(list, T::class.java, rsh).await()
}

suspend inline fun <reified T, R> SQLConnection.asyncInsertObjectBatch(list: List<T>): List<R> {
    return this.insertObjectBatch<T, R>(list, T::class.java).await() ?: listOf()
}

suspend inline fun <reified T> SQLConnection.asyncDeleteById(id: Any): Int {
    return this.deleteById(id, T::class.java).await() ?: 0
}

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