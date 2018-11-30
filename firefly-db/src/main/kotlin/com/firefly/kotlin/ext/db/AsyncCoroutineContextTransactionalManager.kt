package com.firefly.kotlin.ext.db

import com.firefly.db.RecordNotFound
import com.firefly.db.SQLClient
import com.firefly.db.SQLConnection
import com.firefly.kotlin.ext.common.CoroutineLocalContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manage transaction in the HTTP request lifecycle.
 *
 * @author Pengtao Qiu
 */
class AsyncCoroutineContextTransactionalManager(val sqlClient: SQLClient) : AsyncTransactionalManager {

    val currentConnKey = "_currentConnKeyKt"
    val rollbackOnlyKey = "_rollbackOnlyKeyKt"
    val transactionCountKey = "_transactionCountKeyKt"

    override suspend fun getConnection(time: Long, unit: TimeUnit): SQLConnection = withTimeout(unit.toMillis(time)) {
        sqlClient.connection.await()
    }

    override suspend fun getCurrentConnection(time: Long, unit: TimeUnit): SQLConnection? =
        withTimeout(unit.toMillis(time)) {
            CoroutineLocalContext.getAttr<CompletableFuture<SQLConnection>>(currentConnKey)?.await()
        }

    override suspend fun <T> execSQL(time: Long, unit: TimeUnit, handler: suspend (conn: SQLConnection) -> T): T {
        if (isInTransaction()) {
            val conn = getCurrentConnection() ?: throw IllegalStateException("The transaction is not begun")
            return try {
                withTimeout(unit.toMillis(time)) { handler.invoke(conn) }
            } catch (e: RecordNotFound) {
                sysLogger.warn("execute SQL exception. record not found", e)
                throw e
            } catch (e: TimeoutCancellationException) {
                sysLogger.error("execute SQL exception. timeout", e)
                setRollback(true)
                throw e
            } catch (e: Exception) {
                sysLogger.error("execute SQL exception", e)
                setRollback(true)
                throw e
            }
        } else {
            return getConnection().safeUse {
                withTimeout(unit.toMillis(time)) { handler.invoke(it) }
            }
        }
    }

    override suspend fun beginTransaction(): Boolean {
        val count = increaseTransactionCount()
        return if (count == 1) {
            val conn = createConnectionIfEmpty().await()
            conn.beginTransaction().await()
        } else {
            false
        }
    }

    override suspend fun rollbackAndEndTransaction() {
        val count = decreaseTransactionCount()
        if (count <= 0) {
            val rollback = isRollback()
            sysLogger.warn("the transaction rollback -> $rollback, $count")
            if (rollback) {
                getCurrentConnection()?.rollbackAndEndTransaction()?.await()
            } else {
                getCurrentConnection()?.commitAndEndTransaction()?.await()
            }
        }
    }

    override suspend fun commitAndEndTransaction() {
        val count = decreaseTransactionCount()
        if (count <= 0) {
            getCurrentConnection()?.commitAndEndTransaction()?.await()
        }
    }

    private fun createConnectionIfEmpty(): CompletableFuture<SQLConnection> {
        return CoroutineLocalContext.computeIfAbsent(currentConnKey) { sqlClient.connection }
            ?: throw IllegalStateException("Not in coroutine context")
    }

    private fun isInTransaction(): Boolean {
        val count = getTransactionCount()
        return count != null && count > 0
    }

    private fun isRollback(): Boolean {
        val isRollback = CoroutineLocalContext.getAttr<Boolean>(rollbackOnlyKey)
        return isRollback == null || isRollback
    }

    private fun setRollback(rollback: Boolean) {
        CoroutineLocalContext.setAttr(rollbackOnlyKey, rollback)
    }

    private fun increaseTransactionCount(): Int? {
        val count = CoroutineLocalContext.computeIfAbsent(transactionCountKey) { AtomicInteger() }
            ?: throw IllegalStateException("Not in coroutine context")
        return count.incrementAndGet()
    }

    private fun decreaseTransactionCount(): Int {
        val count = CoroutineLocalContext.getAttr<AtomicInteger>(transactionCountKey)
            ?: throw IllegalStateException("The transaction is not begun")
        return count.decrementAndGet()
    }

    private fun getTransactionCount(): Int? {
        return CoroutineLocalContext.getAttr<AtomicInteger>(transactionCountKey)?.get()
    }
}
