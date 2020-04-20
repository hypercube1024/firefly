package com.fireflysource.net.http.server.impl

import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.*
import com.fireflysource.net.http.server.impl.content.provider.DefaultContentProvider
import com.fireflysource.net.http.server.impl.exception.RouterNotCommitException
import com.fireflysource.net.tcp.aio.AioTcpServer
import com.fireflysource.net.tcp.aio.ApplicationProtocol.HTTP1
import com.fireflysource.net.tcp.aio.ApplicationProtocol.HTTP2
import com.fireflysource.net.tcp.onAcceptAsync
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import kotlinx.coroutines.future.await
import java.net.SocketAddress
import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction
import java.util.function.Function

class AsyncHttpServer(val config: HttpConfig = HttpConfig()) : HttpServer, AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(AsyncHttpServer::class.java)
    }

    private val routerManager: RouterManager = AsyncRouterManager(this)
    private val tcpServer = AioTcpServer()
    private var address: SocketAddress? = null
    private var onHeaderComplete: Function<RoutingContext, CompletableFuture<Void>> =
        Function { ctx ->
            if (ctx.expect100Continue()) ctx.response100Continue() else Result.DONE
        }
    private var onException: BiFunction<RoutingContext?, Throwable, CompletableFuture<Void>> =
        BiFunction { ctx, e ->
            log.error(e) { "The internal server error" }
            if (ctx != null && !ctx.response.isCommitted) {
                ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    .setReason(HttpStatus.Code.INTERNAL_SERVER_ERROR.message)
                    .contentProvider(DefaultContentProvider(HttpStatus.INTERNAL_SERVER_ERROR_500, e, ctx))
                    .end()
            } else Result.DONE
        }

    override fun router(): Router = routerManager.register()

    override fun router(id: Int): Router = routerManager.register(id)

    override fun onHeaderComplete(function: Function<RoutingContext, CompletableFuture<Void>>): HttpServer {
        this.onHeaderComplete = function
        return this
    }

    override fun onException(biFunction: BiFunction<RoutingContext?, Throwable, CompletableFuture<Void>>): HttpServer {
        this.onException = biFunction
        return this
    }

    override fun timeout(timeout: Long): HttpServer {
        tcpServer.timeout(timeout)
        return this
    }

    override fun secureEngineFactory(secureEngineFactory: SecureEngineFactory): HttpServer {
        tcpServer.secureEngineFactory(secureEngineFactory)
        return this
    }

    override fun peerHost(peerHost: String): HttpServer {
        tcpServer.peerHost(peerHost)
        return this
    }

    override fun peerPort(peerPort: Int): HttpServer {
        tcpServer.peerPort(peerPort)
        return this
    }

    override fun supportedProtocols(supportedProtocols: MutableList<String>): HttpServer {
        tcpServer.supportedProtocols(supportedProtocols)
        return this
    }

    override fun enableSecureConnection(): HttpServer {
        tcpServer.enableSecureConnection()
        return this
    }

    override fun listen(address: SocketAddress) {
        this.address = address
        start()
    }

    override fun init() {
        val address = this.address
        requireNotNull(address)

        if (config.tcpChannelGroup != null) {
            tcpServer.tcpChannelGroup(config.tcpChannelGroup)
        }

        tcpServer.stopTcpChannelGroup(config.isStopTcpChannelGroup)

        if (config.secureEngineFactory != null) {
            tcpServer.secureEngineFactory(config.secureEngineFactory)
        }

        val listener = object : HttpServerConnection.Listener.Adapter() {
            override fun onHeaderComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return onHeaderComplete.apply(ctx)
            }

            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                val results = routerManager.findRouters(ctx)
                val asyncCtx = ctx as AsyncRoutingContext
                val iterator = results.iterator()
                return if (iterator.hasNext()) {
                    val result = iterator.next()
                    asyncCtx.routerMatchResult = result
                    asyncCtx.routerIterator = iterator
                    (result.router as AsyncRouter).getHandler().apply(ctx).thenCompose {
                        if (ctx.response.isCommitted) Result.DONE
                        else ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500)
                            .setReason(HttpStatus.Code.INTERNAL_SERVER_ERROR.message)
                            .contentProvider(
                                DefaultContentProvider(
                                    HttpStatus.INTERNAL_SERVER_ERROR_500,
                                    RouterNotCommitException("The response does not commit"),
                                    ctx
                                )
                            )
                            .end()
                    }
                } else ctx.setStatus(HttpStatus.NOT_FOUND_404)
                    .setReason(HttpStatus.Code.NOT_FOUND.message)
                    .contentProvider(DefaultContentProvider(HttpStatus.NOT_FOUND_404, null, ctx))
                    .end()
            }

            override fun onException(ctx: RoutingContext?, e: Throwable): CompletableFuture<Void> {
                return onException.apply(ctx, e)
            }
        }

        tcpServer.onAcceptAsync { connection ->
            when (connection.beginHandshake().await()) {
                HTTP2.value -> {
                    val http2Connection = Http2ServerConnection(config, connection)
                    http2Connection.setListener(listener).begin()
                }
                HTTP1.value -> {
                    val http1Connection = Http1ServerConnection(config, connection)
                    http1Connection.setListener(listener).begin()
                }
                else -> {
                    val http1Connection = Http1ServerConnection(config, connection)
                    http1Connection.setListener(listener).begin()
                }
            }
        }

        tcpServer.enableOutputBuffer().listen(address)
    }

    override fun destroy() {
        tcpServer.stop()
    }
}