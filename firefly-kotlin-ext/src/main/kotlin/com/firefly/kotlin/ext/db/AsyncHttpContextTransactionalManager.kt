package com.firefly.kotlin.ext.db

import com.firefly.db.RecordNotFound
import com.firefly.db.SQLClient
import com.firefly.db.SQLConnection
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Manage transaction in the HTTP request lifecycle.
 *
 * @author Pengtao Qiu
 */
class AsyncHttpContextTransactionalManager(
    val requestCtx: CoroutineLocal<RoutingContext>,
    val sqlClient: SQLClient
                                          ) : AsyncTransactionalManager {

    val currentConnKey = "_currentConnKeyKt"
    val inTransactionKey = "_inTransactionKeyKt"
    val rollbackOnlyKey = "_rollbackOnlyKeyKt"

    override suspend fun getConnection(time: Long, unit: TimeUnit): SQLConnection = withTimeout(unit.toMillis(time)) {
        sqlClient.connection.await()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getCurrentConnection(time: Long, unit: TimeUnit): SQLConnection? =
        withTimeout(unit.toMillis(time)) {
            (requestCtx.get()?.attributes?.get(currentConnKey) as CompletableFuture<SQLConnection>?)?.await()
        }

    override suspend fun <T> execSQL(time: Long, unit: TimeUnit, handler: suspend (conn: SQLConnection) -> T): T {
        if (isInTransaction()) {
            val conn = getCurrentConnection() ?: throw IllegalStateException("The transaction is not begun")
            return try {
                val ret = withTimeout(unit.toMillis(time)) { handler.invoke(conn) }
                ret
            } catch (e: RecordNotFound) {
                sysLogger.warn("execute SQL exception. record not found", e)
                requestCtx.get()?.attributes?.put(rollbackOnlyKey, false)
                throw e
            } catch (e: TimeoutCancellationException) {
                sysLogger.error("execute SQL exception. timeout", e)
                requestCtx.get()?.attributes?.put(rollbackOnlyKey, true)
                throw e
            } catch (e: Exception) {
                sysLogger.error("execute SQL exception", e)
                requestCtx.get()?.attributes?.put(rollbackOnlyKey, true)
                throw e
            }
        } else {
            val conn = getConnection()
            return try {
                withTimeout(unit.toMillis(time)) { handler.invoke(conn) }
            } finally {
                conn.close().await()
            }
        }
    }

    override suspend fun beginTransaction(): Boolean {
        val beginNewTransaction: Boolean =
            requestCtx.get()?.attributes?.computeIfAbsent(inTransactionKey) { true } as Boolean
        return if (beginNewTransaction) {
            val conn = createConnectionIfEmpty().await()
            conn.beginTransaction().await()
        } else beginNewTransaction
    }

    override suspend fun rollbackAndEndTransaction() {
        val rollback = isInTransaction() && isRollback()
        sysLogger.warn("the transaction rollback -> $rollback")
        if (rollback) {
            getCurrentConnection()?.rollbackAndEndTransaction()?.await()
        } else {
            getCurrentConnection()?.commitAndEndTransaction()?.await()
        }
    }

    override suspend fun commitAndEndTransaction() {
        if (isInTransaction()) {
            getCurrentConnection()?.commitAndEndTransaction()?.await()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createConnectionIfEmpty(): CompletableFuture<SQLConnection> =
        requestCtx.get()?.attributes?.computeIfAbsent(currentConnKey) {
            sysLogger.debug("init new db connection")
            sqlClient.connection
        } as CompletableFuture<SQLConnection>

    private fun isInTransaction(): Boolean {
        val inTransaction = requestCtx.get()?.attributes?.get(inTransactionKey)
        return inTransaction != null && (inTransaction as Boolean)
    }

    private fun isRollback(): Boolean {
        val isRollback = requestCtx.get()?.attributes?.get(rollbackOnlyKey)
        return isRollback == null || (isRollback as Boolean)
    }
}
