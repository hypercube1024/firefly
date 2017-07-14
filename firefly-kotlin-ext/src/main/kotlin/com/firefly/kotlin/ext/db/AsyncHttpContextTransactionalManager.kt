package com.firefly.kotlin.ext.db

import com.firefly.db.JDBCConnectionUtils.setAutoCommit
import com.firefly.db.JDBCHelper
import com.firefly.db.Transaction
import com.firefly.db.Transaction.Status.INIT
import com.firefly.db.Transaction.Status.START
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.kotlin.ext.http.getAttr
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.experimental.future.await
import java.sql.Connection

/**
 * Manage transaction in the HTTP request lifecycle.
 *
 * @author Pengtao Qiu
 */
class AsyncHttpContextTransactionalManager(val requestCtx: CoroutineLocal<RoutingContext>,
                                           val jdbcHelper: JDBCHelper) : AsynchronousTransactionalManager {

    val jdbcTransactionKey = "_currentJdbcTransaction"

    suspend override fun asyncGetConnection(): Connection = getTransaction()?.connection ?: jdbcHelper.asyncGetConnection().await()

    suspend override fun asyncBeginTransaction() {
        createTransactionIfEmpty().asyncBeginTransaction()
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
        createTransactionIfEmpty().beginTransaction()
    }

    private fun getTransaction(): AsynchronousTransaction? = requestCtx.get()?.getAttr<AsynchronousTransaction>(jdbcTransactionKey)

    private fun createTransactionIfEmpty() = requestCtx.get()?.attributes?.computeIfAbsent(jdbcTransactionKey) {
        AsynchronousTransaction(jdbcHelper)
    } as AsynchronousTransaction

}

class AsynchronousTransaction(val jdbcHelper: JDBCHelper) : Transaction(jdbcHelper.dataSource) {

    @Synchronized
    suspend fun asyncBeginTransaction(): Unit {
        if (status == INIT) {
            connection = jdbcHelper.asyncGetConnection().await()
            setAutoCommit(connection, false)
            status = START
        }
        count++
        log.debug("begin transaction asynchronously {}", count)
    }
}
