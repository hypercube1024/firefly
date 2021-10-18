package com.fireflysource.net.http.server.impl

import com.fireflysource.common.coroutine.asVoidFuture
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientContentHandlerFactory
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.client.impl.content.provider.FileContentProvider
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.server.HttpServerContentProviderFactory
import com.fireflysource.net.http.server.HttpServerFactory
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
                connection.coroutineScope.launch { this.buildHttpTunnel(connection, targetAddress) }.asVoidFuture()
            }
            .onHeaderComplete { ctx ->
                if (ctx.expect100Continue()) {
                    ctx.response100Continue()
                } else if (ctx.method == HttpMethod.CONNECT.value) {
                    Result.DONE
                } else {
                    val path = Paths.get(tempPath, "http-proxy-server-body-${UUID.randomUUID()}")
                    val handler = FileContentHandler(
                        path,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.READ,
                        StandardOpenOption.WRITE
                    )
                    ctx.contentHandler(handler)
                    Result.DONE
                }
            }
            .router().path("*").asyncHandler { ctx ->
                val serverContentHandler = ctx.request.contentHandler as FileContentHandler
                val clientBodyPath = Paths.get(tempPath, "http-proxy-client-body-${UUID.randomUUID()}")
                try {
                    log.debug {
                        "server receives content path: ${serverContentHandler.path}, " +
                                "length: ${Files.size(serverContentHandler.path)}"
                    }

                    val clientBodyContentHandler = HttpClientContentHandlerFactory.fileHandler(
                        clientBodyPath,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.READ,
                        StandardOpenOption.WRITE
                    )
                    val response = httpClient.request(ctx.method, ctx.uri)
                        .addAll(ctx.httpFields)
                        .contentProvider(FileContentProvider(serverContentHandler.path, StandardOpenOption.READ))
                        .contentHandler(clientBodyContentHandler)
                        .submit().await()
                    log.debug {
                        "client receives content path: $clientBodyPath, " +
                                "length: ${Files.size(clientBodyPath)}, " +
                                "response size: ${response.contentLength}"
                    }

                    ctx.setStatus(response.status)
                    ctx.response.httpFields.addAll(response.httpFields)
                    ctx.contentProvider(
                        HttpServerContentProviderFactory.fileBody(
                            clientBodyPath,
                            StandardOpenOption.READ
                        )
                    )
                    ctx.end().await()
                } finally {
                    Files.delete(serverContentHandler.path)
                    Files.delete(clientBodyPath)
                }
            }
    }

    private suspend fun CoroutineScope.buildHttpTunnel(connection: TcpConnection, targetAddress: InetSocketAddress) {
        val targetConnection = tcpClient.connect(targetAddress).await()
        val readFromClientJob = launch {
            while (true) {
                val r = runCatching {
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
        val writeToClientJob = launch {
            while (true) {
                val r = runCatching {
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
        runCatching {
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