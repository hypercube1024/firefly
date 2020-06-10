package com.fireflysource.net.http.server.impl

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.*
import com.fireflysource.net.http.server.impl.content.provider.DefaultContentProvider
import com.fireflysource.net.http.server.impl.exception.RouterNotCommitException
import com.fireflysource.net.http.server.impl.router.AsyncRouterManager
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AioTcpServer
import com.fireflysource.net.tcp.aio.ApplicationProtocol.HTTP1
import com.fireflysource.net.tcp.aio.ApplicationProtocol.HTTP2
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import com.fireflysource.net.websocket.server.AsyncWebSocketManager
import com.fireflysource.net.websocket.server.AsyncWebSocketServerConnectionBuilder
import com.fireflysource.net.websocket.server.WebSocketManager
import com.fireflysource.net.websocket.server.WebSocketServerConnectionBuilder
import java.net.SocketAddress
import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction
import java.util.function.Function

class AsyncHttpServer(val config: HttpConfig = HttpConfig()) : HttpServer, AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(AsyncHttpServer::class.java)
    }

    private val routerManager: RouterManager = AsyncRouterManager(this)
    private val webSocketManager: WebSocketManager = AsyncWebSocketManager()
    private val tcpServer = AioTcpServer()
    private var address: SocketAddress? = null
    private var onHeaderComplete: Function<RoutingContext, CompletableFuture<Void>> = Function { ctx ->
        if (ctx.expect100Continue()) ctx.response100Continue() else Result.DONE
    }
    private var onException: BiFunction<RoutingContext?, Throwable, CompletableFuture<Void>> = BiFunction { ctx, e ->
        log.error(e) { "The internal server error" }
        if (ctx != null && !ctx.response.isCommitted) {
            if (e is BadMessageException) {
                ctx.setStatus(e.code)
                    .contentProvider(DefaultContentProvider(e.code, e, ctx))
                    .end()
                    .thenCompose { ctx.connection.closeFuture() }
            } else {
                ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    .setReason(HttpStatus.Code.INTERNAL_SERVER_ERROR.message)
                    .contentProvider(DefaultContentProvider(HttpStatus.INTERNAL_SERVER_ERROR_500, e, ctx))
                    .end()
            }
        } else Result.DONE
    }
    private var onRouterNotFound: Function<RoutingContext, CompletableFuture<Void>> = Function { ctx ->
        ctx.setStatus(HttpStatus.NOT_FOUND_404)
            .setReason(HttpStatus.Code.NOT_FOUND.message)
            .contentProvider(DefaultContentProvider(HttpStatus.NOT_FOUND_404, null, ctx))
            .end()
    }
    private var onRouterComplete: Function<RoutingContext, CompletableFuture<Void>> = Function { ctx ->
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


    override fun router(): Router = routerManager.register()

    override fun router(id: Int): Router = routerManager.register(id)

    override fun websocket(): WebSocketServerConnectionBuilder {
        return AsyncWebSocketServerConnectionBuilder(this, webSocketManager)
    }

    override fun websocket(path: String): WebSocketServerConnectionBuilder {
        return AsyncWebSocketServerConnectionBuilder(this, webSocketManager).url(path)
    }

    override fun onHeaderComplete(function: Function<RoutingContext, CompletableFuture<Void>>): HttpServer {
        this.onHeaderComplete = function
        return this
    }

    override fun onException(biFunction: BiFunction<RoutingContext?, Throwable, CompletableFuture<Void>>): HttpServer {
        this.onException = biFunction
        return this
    }

    override fun onRouterComplete(function: Function<RoutingContext, CompletableFuture<Void>>): HttpServer {
        this.onRouterComplete = function
        return this
    }

    override fun onRouterNotFound(function: Function<RoutingContext, CompletableFuture<Void>>): HttpServer {
        this.onRouterNotFound = function
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
        require(config.maxRequestBodySize >= config.maxUploadFileSize) { "The max request size must be greater than the max file size." }
        require(config.maxUploadFileSize >= config.uploadFileSizeThreshold) { "The max file size must be greater than the file size threshold." }

        val address = this.address
        requireNotNull(address)

        if (config.tcpChannelGroup != null) {
            tcpServer.tcpChannelGroup(config.tcpChannelGroup)
        }

        tcpServer.stopTcpChannelGroup(config.isStopTcpChannelGroup)

        if (config.secureEngineFactory != null) {
            tcpServer.secureEngineFactory(config.secureEngineFactory)
        }

        val listener = AsyncHttpServerConnectionListener(
            routerManager, onHeaderComplete, onException, onRouterNotFound, onRouterComplete, webSocketManager
        )

        tcpServer.onAccept { connection ->
            if (connection.isSecureConnection) {
                connection.beginHandshake().thenAccept { protocol ->
                    when (protocol) {
                        HTTP2.value -> createHttp2Connection(connection, listener)
                        HTTP1.value -> createHttp1Connection(connection, listener)
                        else -> createHttp1Connection(connection, listener)
                    }
                }.exceptionallyAccept { e ->
                    log.error(e) { "TLS handshake exception. id: ${connection.id}" }
                    connection.close()
                }
            } else createHttp1Connection(connection, listener)
        }

        tcpServer.enableOutputBuffer().listen(address)
    }

    private fun createHttp2Connection(connection: TcpConnection, listener: HttpServerConnection.Listener.Adapter) {
        val http2Connection = Http2ServerConnection(config, connection)
        http2Connection.setListener(listener).begin()
    }

    private fun createHttp1Connection(connection: TcpConnection, listener: HttpServerConnection.Listener.Adapter) {
        val http1Connection = Http1ServerConnection(config, connection)
        http1Connection.setListener(listener).begin()
    }

    override fun destroy() {
        tcpServer.stop()
    }
}