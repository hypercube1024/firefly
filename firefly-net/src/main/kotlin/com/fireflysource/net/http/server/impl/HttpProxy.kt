package com.fireflysource.net.http.server.impl

import com.fireflysource.common.coroutine.asVoidFuture
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.http.server.impl.router.asyncHandler
import com.fireflysource.net.tcp.TcpClientFactory
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.net.SocketAddress
import java.util.concurrent.CompletableFuture

class HttpProxy(httpConfig: HttpConfig = HttpConfig()) {

    companion object {
        private val log = SystemLogger.create(HttpProxy::class.java)
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
                connection.coroutineScope.launch {
                    var targetConnection = tcpClient.connect(targetAddress).await()
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
                    targetConnection.closeAsync()
                    connection.closeAsync()
                }.asVoidFuture()
            }
            .router().path("*").asyncHandler { ctx ->
                val response = httpClient.request(ctx.method, ctx.uri)
                    .addAll(ctx.httpFields)
                    .body(BufferUtils.merge(ctx.body))
                    .submit().await()
                ctx.setStatus(response.status)
                ctx.response.httpFields.addAll(response.httpFields)
                ctx.write(BufferUtils.merge(response.body))
                ctx.end()
            }
    }

    fun listen(address: SocketAddress) {
        server.listen(address)
    }

    fun listen(host: String, port: Int) {
        server.listen(host, port)
    }
}