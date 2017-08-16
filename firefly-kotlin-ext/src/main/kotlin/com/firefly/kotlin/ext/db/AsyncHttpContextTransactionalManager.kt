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
                                           val sqlClient: SQLClient) {

    val transactionKey = "_currentTransaction"

    suspend fun getConnection(): SQLConnection {
        if (requestCtx.get() == null) {
            return sqlClient.connection.await()
        } else {
            return createConnectionIfEmpty().await()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createConnectionIfEmpty(): CompletableFuture<SQLConnection> = requestCtx.get()?.attributes?.computeIfAbsent(transactionKey) {
        sqlClient.connection
    } as CompletableFuture<SQLConnection>

}
