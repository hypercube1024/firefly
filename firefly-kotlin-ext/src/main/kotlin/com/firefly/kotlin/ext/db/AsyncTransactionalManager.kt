package com.firefly.kotlin.ext.db

import com.firefly.db.SQLConnection

/**
 * @author Pengtao Qiu
 */
interface AsyncTransactionalManager {

    suspend fun getConnection(): SQLConnection

    suspend fun <T> execSQL(handler: suspend (conn: SQLConnection) -> T): T?

}