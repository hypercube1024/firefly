package com.fireflysource.net.http.server.impl

import com.fireflysource.common.annotation.NoArg
import com.fireflysource.common.concurrent.exceptionallyCompose
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.codec.URIUtils
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RouterManager
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.http.server.impl.router.AsyncRouter
import com.fireflysource.net.http.server.impl.router.AsyncRoutingContext
import com.fireflysource.net.websocket.server.WebSocketManager
import com.fireflysource.net.websocket.server.WebSocketServerConnectionHandler
import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction
import java.util.function.Function

/**
 * @author Pengtao Qiu
 */
@NoArg
class AsyncHttpServerConnectionListener(
    private val routerManager: RouterManager,
    private val onHeaderComplete: Function<RoutingContext, CompletableFuture<Void>>,
    private val onException: BiFunction<RoutingContext?, Throwable, CompletableFuture<Void>>,
    private val onRouterNotFound: Function<RoutingContext, CompletableFuture<Void>>,
    private val onRouterComplete: Function<RoutingContext, CompletableFuture<Void>>,
    private val webSocketManager: WebSocketManager
) : HttpServerConnection.Listener.Adapter() {

    override fun onHeaderComplete(ctx: RoutingContext): CompletableFuture<Void> {
        return onHeaderComplete.apply(ctx)
    }

    override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
        if (ctx.response.isCommitted) return Result.DONE

        val results = routerManager.findRouters(ctx)
        val asyncCtx = ctx as AsyncRoutingContext
        val iterator = results.iterator()
        return if (iterator.hasNext()) {
            val result = iterator.next()
            asyncCtx.routerMatchResult = result
            asyncCtx.routerIterator = iterator
            (result.router as AsyncRouter).getHandler()
                .apply(ctx)
                .thenCompose { handleRouterComplete(ctx) }
                .exceptionallyCompose { handleRouterException(ctx, it) }
        } else handleRouterNotFound(ctx)
    }

    override fun onException(ctx: RoutingContext?, e: Throwable): CompletableFuture<Void> {
        return onException.apply(ctx, e)
    }

    override fun onWebSocketHandshake(ctx: RoutingContext): CompletableFuture<WebSocketServerConnectionHandler> {
        val path = URIUtils.canonicalPath(ctx.uri.decodedPath)
        val handler = webSocketManager.findWebSocketHandler(path)
        val future = CompletableFuture<WebSocketServerConnectionHandler>()
        if (handler != null) {
            future.complete(handler)
        } else {
            future.completeExceptionally(
                BadMessageException(
                    HttpStatus.BAD_REQUEST_400,
                    "The websocket handler is not register"
                )
            )
        }
        return future
    }

    private fun handleRouterNotFound(ctx: RoutingContext): CompletableFuture<Void> {
        return onRouterNotFound.apply(ctx)
    }

    private fun handleRouterException(ctx: RoutingContext, e: Throwable): CompletableFuture<Void> {
        return onException.apply(ctx, e)
    }

    private fun handleRouterComplete(ctx: RoutingContext): CompletableFuture<Void> {
        return onRouterComplete.apply(ctx)
    }

}