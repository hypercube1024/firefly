package com.firefly.kotlin.ext.db

import com.firefly.db.TransactionalManager
import java.sql.Connection

/**
 * @author Pengtao Qiu
 */
interface AsynchronousTransactionalManager : TransactionalManager {
    suspend fun asyncGetConnection(): Connection

    suspend fun asyncBeginTransaction(): Unit

    suspend fun asyncEndTransaction(): Unit
}