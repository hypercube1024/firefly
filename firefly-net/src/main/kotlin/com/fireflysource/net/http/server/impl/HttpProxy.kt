package com.fireflysource.net.http.server.impl

import com.fireflysource.common.coroutine.asVoidFuture
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.http.server.impl.router.asyncHandler
import com.fireflysource.net.tcp.TcpClientFactory
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

class HttpProxy(httpConfig: HttpConfig) {

    private val server = HttpServerFactory.create(httpConfig)
    private val tcpClient = TcpClientFactory.create()

    init {
        server
            .onAcceptHttpTunnel { CompletableFuture.completedFuture(true) }
            .onHttpTunnelHandshakeComplete { connection, targetAddress ->
                connection.coroutineScope.launch {
                    var targetConnection = tcpClient.connect(targetAddress).await()
                    val readJob = launch {
                        while (true) {
                            val data = connection.read().await()
                            targetConnection.write(data)
                        }
                    }
                    val writeJob = launch {
                        while (true) {
                            val data = targetConnection.read().await()
                            connection.write(data)
                        }
                    }
                    readJob.join()
                    writeJob.join()
                }.asVoidFuture()
            }
            .router().path("*").asyncHandler { ctx ->

            }
    }
}