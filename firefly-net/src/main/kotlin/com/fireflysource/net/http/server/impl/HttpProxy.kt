package com.fireflysource.net.http.server.impl

import com.fireflysource.common.coroutine.asVoidFuture
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.deleteIfExistsAsync
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientContentHandlerFactory
import com.fireflysource.net.http.client.HttpClientContentProviderFactory
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpHeaderValue
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.server.HttpServerContentHandlerFactory
import com.fireflysource.net.http.server.HttpServerContentProviderFactory
import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.http.server.impl.content.handler.ByteBufferContentHandler
import com.fireflysource.net.http.server.impl.content.handler.FileContentHandler
import com.fireflysource.net.http.server.impl.router.asyncHandler
import com.fireflysource.net.tcp.TcpClientFactory
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.CoroutineScope
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

class HttpProxy(httpConfig: HttpConfig = HttpConfig()) {

    companion object {
        private val log = SystemLogger.create(HttpProxy::class.java)
        private val tempPath = System.getProperty("java.io.tmpdir")
    }

    private val server = HttpServerFactory.create(httpConfig)
    private val tcpClient = TcpClientFactory.create()
    private val httpClient = HttpClientFactory.create(httpConfig)

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
                    setServerContentHandler(ctx, httpConfig)
                    Result.DONE
                }
            }
            .router().path("*").asyncHandler { ctx ->
                var serverBodyPath: Path? = null
                var clientBodyPath: Path? = null

                try {
                    fun createFileHandler(): HttpClientContentHandler {
                        clientBodyPath =
                            Paths.get(tempPath, "com.fireflysource.http.proxy", "client-body-${UUID.randomUUID()}")
                        return HttpClientContentHandlerFactory.fileHandler(
                            clientBodyPath,
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.READ,
                            StandardOpenOption.WRITE
                        )
                    }

                    val clientContentProvider = when (val serverContentHandler = ctx.request.contentHandler) {
                        is FileContentHandler -> {
                            serverBodyPath = serverContentHandler.path
                            HttpClientContentProviderFactory.fileBody(
                                serverContentHandler.path,
                                StandardOpenOption.READ
                            )
                        }
                        is ByteBufferContentHandler -> HttpClientContentProviderFactory.bytesBody(
                            BufferUtils.merge(
                                serverContentHandler.getByteBuffers()
                            )
                        )
                        else -> throw IllegalStateException("The HTTP proxy content handler type error.")
                    }

                    val response = httpClient.request(ctx.method, ctx.uri)
                        .addAll(ctx.httpFields)
                        .contentProvider(clientContentProvider)
                        .onHeaderComplete { request, response ->
                            val maxResponseBody = 10 * 1024 * 1024L
                            val clientBodyContentHandler =
                                if (response.contentLength > maxResponseBody) {
                                    createFileHandler()
                                } else if (response.httpFields.contains(
                                        HttpHeader.TRANSFER_ENCODING,
                                        HttpHeaderValue.CHUNKED.value
                                    )
                                ) {
                                    createFileHandler()
                                } else {
                                    HttpClientContentHandlerFactory.bytesHandler(maxResponseBody)
                                }
                            request.contentHandler = clientBodyContentHandler
                            response.contentHandler = clientBodyContentHandler
                        }
                        .submit().await()
                    log.debug {
                        "client receives content path: $clientBodyPath, " +
                                "length: ${Files.size(clientBodyPath)}, " +
                                "response size: ${response.contentLength}"
                    }

                    ctx.setStatus(response.status)
                    ctx.response.httpFields.addAll(response.httpFields)
                    when (val responseContentHandler = response.contentHandler) {
                        is com.fireflysource.net.http.client.impl.content.handler.FileContentHandler -> {
                            ctx.contentProvider(
                                HttpServerContentProviderFactory.fileBody(
                                    clientBodyPath,
                                    StandardOpenOption.READ
                                )
                            )
                        }
                        is com.fireflysource.net.http.client.impl.content.handler.ByteBufferContentHandler -> {
                            ctx.contentProvider(
                                HttpServerContentProviderFactory.bytesBody(
                                    BufferUtils.merge(
                                        responseContentHandler.getByteBuffers()
                                    )
                                )
                            )
                        }
                        else -> throw IllegalStateException("The HTTP client response content handler type error.")
                    }
                    ctx.end().await()
                } finally {
                    val serverPath = serverBodyPath
                    if (serverPath != null) {
                        deleteIfExistsAsync(serverPath)
                    }
                    val clientPath = clientBodyPath
                    if (clientPath != null) {
                        deleteIfExistsAsync(clientPath)
                    }
                }
            }
    }

    private fun setServerContentHandler(ctx: RoutingContext, httpConfig: HttpConfig) {
        fun setFileHandler() {
            val path =
                Paths.get(tempPath, "com.fireflysource.http.proxy", "server-body-${UUID.randomUUID()}")
            val handler = HttpServerContentHandlerFactory.fileHandler(
                path,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            )
            ctx.contentHandler(handler)
        }

        fun setBytesHandler() {
            ctx.contentHandler(HttpServerContentHandlerFactory.bytesHandler(httpConfig.maxRequestBodySize))
        }

        if (ctx.contentLength <= 0) {
            if (ctx.httpFields.contains(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED.value)) {
                setFileHandler()
            } else {
                setBytesHandler()
            }
        } else if (ctx.contentLength > httpConfig.maxRequestBodySize) {
            setFileHandler()
        } else {
            setBytesHandler()
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

    fun listen(address: SocketAddress) {
        server.listen(address)
    }

    fun listen(host: String, port: Int) {
        server.listen(host, port)
    }
}