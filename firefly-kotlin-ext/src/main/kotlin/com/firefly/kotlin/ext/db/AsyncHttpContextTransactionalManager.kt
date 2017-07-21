package com.firefly.kotlin.ext.db

import com.firefly.db.JDBCHelper
import com.firefly.db.Transaction
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.kotlin.ext.http.getAttr
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.experimental.future.await
import java.sql.Connection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier

/**
 * Manage transaction in the HTTP request lifecycle.
 *
 * @author Pengtao Qiu
 */
class AsyncHttpContextTransactionalManager(val requestCtx: CoroutineLocal<RoutingContext>,
                                           val jdbcHelper: JDBCHelper) : AsynchronousTransactionalManager {

    val jdbcTransactionKey = "_currentJdbcTransaction"
    val idGenerator = AtomicLong()

    suspend override fun asyncGetConnection(): Connection = getTransaction()?.connection ?: jdbcHelper.asyncGetConnection().await()

    suspend override fun asyncBeginTransaction() {
        createTransactionIfEmpty()?.asyncBeginTransaction()
    }

    suspend override fun asyncEndTransaction() {
        if (getTransaction()?.asyncEndTransaction() ?: false) {
            requestCtx.get()?.removeAttribute(jdbcTransactionKey)
        }
    }

    override fun getConnection(): Connection = getTransaction()?.connection ?: jdbcHelper.connection

    override fun commit() {
        getTransaction()?.commit()
    }

    override fun rollback() {
        getTransaction()?.rollback()
    }

    override fun endTransaction() {
        if (getTransaction()?.endTransaction() ?: false) {
            requestCtx.get()?.removeAttribute(jdbcTransactionKey)
        }
    }

    override fun isTransactionBegin(): Boolean = getTransaction() != null

    override fun beginTransaction() {
        createTransactionIfEmpty()?.beginTransaction()
    }

    override fun getCurrentTransactionId(): Long = getTransaction()?.id ?: -1

    private fun getTransaction(): AsynchronousTransaction? = requestCtx.get()?.getAttr<AsynchronousTransaction>(jdbcTransactionKey)

    private fun createTransactionIfEmpty() = requestCtx.get()?.attributes?.computeIfAbsent(jdbcTransactionKey) {
        AsynchronousTransaction(jdbcHelper, idGenerator.incrementAndGet())
    } as AsynchronousTransaction?

}

class AsynchronousTransaction(val jdbcHelper: JDBCHelper, id: Long) : Transaction(jdbcHelper.dataSource, id) {

    suspend fun asyncBeginTransaction(): Unit {
        CompletableFuture.runAsync(Runnable { beginTransaction() }, jdbcHelper.executorService).await()
        log.debug("begin transaction asynchronously, id: {}, count: {}", id, count)
    }

    suspend fun asyncEndTransaction(): Boolean {
        val ret = CompletableFuture.supplyAsync(Supplier { endTransaction() }, jdbcHelper.executorService).await()
        log.debug("end transaction asynchronously, id: {}, count: {}", id, count)
        return ret
    }
}
