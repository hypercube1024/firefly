package com.firefly.kotlin.ext.db

import com.firefly.db.SQLConnection
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
                           ): T

    suspend fun beginTransaction(): Boolean

    suspend fun rollbackAndEndTransaction()

    suspend fun commitAndEndTransaction()

}