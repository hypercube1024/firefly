package com.firefly.kotlin.ext.db

import com.firefly.db.SQLConnection
import kotlinx.coroutines.experimental.future.await

/**
 * @author Pengtao Qiu
 */
interface AsyncTransactionalManager {

    suspend fun getConnection(): SQLConnection

    suspend fun getCurrentConnection(): SQLConnection?

    suspend fun <T> execSQL(handler: suspend (conn: SQLConnection) -> T): T = getConnection().execSQL(handler)

    suspend fun beginTransaction(): Boolean = getConnection().beginTransaction().await()

    suspend fun rollbackAndEndTransaction() {
        getConnection().rollbackAndEndTransaction().await()
    }

    suspend fun commitAndEndTransaction() {
        getConnection().commitAndEndTransaction().await()
    }

}