package com.firefly.kotlin.ext.db

import com.firefly.db.SQLClient
import com.firefly.db.SQLConnection
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.experimental.future.await
import java.util.concurrent.CompletableFuture

/**
 * Manage transaction in the HTTP request lifecycle.
 *
 * @author Pengtao Qiu
 */
class AsyncHttpContextTransactionalManager(val requestCtx: CoroutineLocal<RoutingContext>,
                                           val sqlClient: SQLClient) : AsyncTransactionalManager {

    val transactionKey = "_currentKotlinTransaction"

    override suspend fun getConnection(): SQLConnection {
        return if (requestCtx.get() == null) {
            sysLogger.debug("get new db connection from pool")
            sqlClient.connection.await()
        } else {
            var sqlConn = createConnectionIfEmpty().await()
            if (sqlConn.connection.isClosed) {
                val future = sqlClient.connection
                sqlConn = future.await()
                requestCtx.get()?.attributes?.put(transactionKey, future)
                sqlConn
            } else {
                sqlConn
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getCurrentConnection(): SQLConnection? {
        val future = requestCtx.get()?.attributes?.get(transactionKey) as CompletableFuture<SQLConnection>?
        return future?.await()
    }

    @Suppress("UNCHECKED_CAST")
    private fun createConnectionIfEmpty(): CompletableFuture<SQLConnection> = requestCtx.get()?.attributes?.computeIfAbsent(transactionKey) {
        sysLogger.debug("init new db connection")
        sqlClient.connection
    } as CompletableFuture<SQLConnection>

}
