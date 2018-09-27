package com.firefly.kotlin.ext.db

import com.firefly.db.SQLConnection
import kotlinx.coroutines.experimental.future.await
import java.util.concurrent.TimeUnit

/**
 * @author Pengtao Qiu
 */
interface AsyncTransactionalManager {

    suspend fun getConnection(time: Long = 10000L, unit: TimeUnit = TimeUnit.MILLISECONDS): SQLConnection

    suspend fun getCurrentConnection(time: Long = 10000L, unit: TimeUnit = TimeUnit.MILLISECONDS): SQLConnection?

    suspend fun <T> execSQL(
        time: Long = 10000L,
        unit: TimeUnit = TimeUnit.MILLISECONDS,
        handler: suspend (conn: SQLConnection) -> T
                           ): T = getConnection().execSQL(time, unit, handler)

    suspend fun beginTransaction(): Boolean = getConnection().beginTransaction().await()

    suspend fun rollbackAndEndTransaction() {
        getConnection().rollbackAndEndTransaction().await()
    }

    suspend fun commitAndEndTransaction() {
        getConnection().commitAndEndTransaction().await()
    }

}