package com.fireflysource.net.http.server.impl

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.coroutine.asVoidFuture
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.deleteIfExistsAsync
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.*
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpHeaderValue
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.server.*
import com.fireflysource.net.http.server.impl.content.handler.ByteBufferContentHandler
import com.fireflysource.net.http.server.impl.content.handler.FileContentHandler
import com.fireflysource.net.http.server.impl.router.asyncHandler
import com.fireflysource.net.tcp.TcpClientFactory
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.CompletableFuture

class AsyncHttpProxy(httpConfig: HttpConfig = HttpConfig()) : AbstractLifeCycle(), HttpProxy {

    companion object {
        private val log = SystemLogger.create(AsyncHttpProxy::class.java)
        private val tempPath = System.getProperty("java.io.tmpdir")
        private const val httpBodyDir = "com.fireflysource.http.proxy"
        private const val httpServerBodyPathKey = "httpProxyServerContentPath"
        private const val httpClientBodyPathKey = "httpProxyClientContentPath"
    }

    private val server = HttpServerFactory.create(httpConfig)
    private val tcpClient = TcpClientFactory.create()
    private val httpClient = HttpClientFactory.create(httpConfig)
    private val httpProxyBodySizeThreshold = httpConfig.httpProxyBodySizeThreshold

    init {
        server
            .onAcceptHttpTunnel { request ->
                log.info("Accept http tunnel handshake. uri: ${request.uri}")
                CompletableFuture.completedFuture(true)
            }
            .onHttpTunnelHandshakeComplete { connection, targetAddress ->
                log.info("HTTP tunnel handshake success. target: $targetAddress")
                connection.coroutineScope.launch { buildHttpTunnel(this, connection, targetAddress) }.asVoidFuture()
            }
            .onHeaderComplete { ctx ->
                if (ctx.expect100Continue()) {
                    ctx.response100Continue()
                } else if (ctx.method == HttpMethod.CONNECT.value) {
                    Result.DONE
                } else {
                    setServerContentHandler(ctx)
                    Result.DONE
                }
            }
            .router().path("*").asyncHandler { buildNonSecureHttpHandler(it) }
        val tempDir = Paths.get(tempPath, httpBodyDir)
        if (!Files.exists(tempDir)) {
            Files.createDirectory(tempDir)
        }
        log.info("HTTP proxy content temp file path: $tempDir")
        start()
    }

    private suspend fun buildNonSecureHttpHandler(ctx: RoutingContext) {
        try {
            val response = httpClient.request(ctx.method, ctx.uri)
                .addAll(ctx.httpFields)
                .contentProvider(createClientContentProvider(ctx.request.contentHandler))
                .onHeaderComplete { request, response -> setClientContentHandler(request, response, ctx) }
                .submit().await()

            ctx.setStatus(response.status)
                .addAll(response.httpFields)
                .contentProvider(createServerContentProvider(response.contentHandler))
                .end().await()
        } finally {
            clearBodyTempFile(ctx)
        }
    }

    private fun setClientContentHandler(
        request: HttpClientRequest,
        response: HttpClientResponse,
        ctx: RoutingContext
    ) {
        val clientBodyContentHandler = createClientContentHandler(response, ctx)
        request.contentHandler = clientBodyContentHandler
        response.contentHandler = clientBodyContentHandler
    }

    private fun createClientContentHandler(
        response: HttpClientResponse,
        ctx: RoutingContext
    ): HttpClientContentHandler {
        fun createFileHandler(): HttpClientContentHandler {
            val clientBodyPath =
                Paths.get(tempPath, httpBodyDir, "client-body-${UUID.randomUUID()}")
            ctx.setAttribute(httpClientBodyPathKey, clientBodyPath)
            return HttpClientContentHandlerFactory.fileHandler(
                clientBodyPath,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            )
        }
        return if (response.contentLength > httpProxyBodySizeThreshold) {
            createFileHandler()
        } else if (response.httpFields.contains(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED.value)) {
            createFileHandler()
        } else {
            HttpClientContentHandlerFactory.bytesHandler(httpProxyBodySizeThreshold)
        }
    }

    private fun createClientContentProvider(serverContentHandler: HttpServerContentHandler) =
        when (serverContentHandler) {
            is FileContentHandler -> HttpClientContentProviderFactory.fileBody(
                serverContentHandler.path,
                StandardOpenOption.READ
            )
            is ByteBufferContentHandler -> HttpClientContentProviderFactory.bytesBody(
                BufferUtils.merge(serverContentHandler.getByteBuffers())
            )
            else -> throw IllegalStateException("The HTTP proxy content handler type error.")
        }

    private fun createServerContentProvider(clientContentHandler: HttpClientContentHandler) =
        when (clientContentHandler) {
            is com.fireflysource.net.http.client.impl.content.handler.FileContentHandler -> HttpServerContentProviderFactory.fileBody(
                clientContentHandler.path,
                StandardOpenOption.READ
            )
            is com.fireflysource.net.http.client.impl.content.handler.ByteBufferContentHandler -> HttpServerContentProviderFactory.bytesBody(
                BufferUtils.merge(clientContentHandler.getByteBuffers())
            )
            else -> throw IllegalStateException("The HTTP client response content handler type error.")
        }


    private fun setServerContentHandler(ctx: RoutingContext) {
        fun setFileHandler() {
            val path =
                Paths.get(tempPath, httpBodyDir, "server-body-${UUID.randomUUID()}")
            val handler = HttpServerContentHandlerFactory.fileHandler(
                path,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            )
            ctx.setAttribute(httpServerBodyPathKey, path)
            ctx.contentHandler(handler)
        }

        fun setBytesHandler() {
            ctx.contentHandler(HttpServerContentHandlerFactory.bytesHandler(httpProxyBodySizeThreshold))
        }

        if (ctx.contentLength > httpProxyBodySizeThreshold) {
            setFileHandler()
        } else if (ctx.httpFields.contains(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED.value)) {
            setFileHandler()
        } else {
            setBytesHandler()
        }
    }

    private fun clearBodyTempFile(ctx: RoutingContext) {
        ctx.getAttribute(httpServerBodyPathKey)?.let {
            deleteIfExistsAsync(it as Path).asCompletableFuture()
                .thenAccept { success -> log.debug("delete server body temp file. $success") }
                .exceptionallyAccept { e -> log.error(e) { "delete server body temp file exception." } }
        }
        ctx.getAttribute(httpClientBodyPathKey)?.let {
            deleteIfExistsAsync(it as Path).asCompletableFuture()
                .thenAccept { success -> log.debug("delete client body temp file. $success") }
                .exceptionallyAccept { e -> log.error(e) { "delete client body temp file exception." } }
        }
    }

    private suspend fun buildHttpTunnel(
        coroutineScope: CoroutineScope,
        connection: TcpConnection,
        targetAddress: InetSocketAddress
    ) {
        val targetConnection = tcpClient.connect(targetAddress).await()
        val readFromClientJob = coroutineScope.launch {
            while (true) {
                val r = this.runCatching {
                    val data = connection.read().await()
                    val size = targetConnection.write(data).await()
                    log.debug("write to target: $size")
                }
                if (r.isFailure) {
                    log.error("read from client job failure", r.exceptionOrNull())
                    break
                }
            }
        }
        val writeToClientJob = coroutineScope.launch {
            while (true) {
                val r = this.runCatching {
                    val data = targetConnection.read().await()
                    val size = connection.write(data).await()
                    connection.flush().await()
                    log.debug("write to client: $size")
                }
                if (r.isFailure) {
                    log.error("write to client job failure", r.exceptionOrNull())
                    break
                }
            }
        }
        coroutineScope.runCatching {
            readFromClientJob.join()
            log.info("HTTP tunnel read job exit.")
            writeToClientJob.join()
            log.info("HTTP tunnel write job exit.")
        }
        targetConnection.closeAsync().await()
        connection.closeAsync().await()
    }

    override fun listen(address: SocketAddress) {
        server.listen(address)
    }

    override fun init() {
    }

    override fun destroy() {
        server.stop()
        tcpClient.stop()
        httpClient.stop()
    }
}