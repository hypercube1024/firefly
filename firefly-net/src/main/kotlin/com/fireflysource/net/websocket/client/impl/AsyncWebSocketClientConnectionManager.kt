package com.fireflysource.net.websocket.client.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.net.http.client.impl.Http1ClientConnection
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.websocket.client.WebSocketClientConnectionManager
import com.fireflysource.net.websocket.client.WebSocketClientRequest
import com.fireflysource.net.websocket.common.WebSocketConnection
import com.fireflysource.net.websocket.common.exception.WebSocketException
import com.fireflysource.net.websocket.common.model.WebSocketBehavior
import java.util.concurrent.CompletableFuture

/**
 * @author Pengtao Qiu
 */
class AsyncWebSocketClientConnectionManager(
    private val config: HttpConfig,
    private val tcpClient: TcpClient,
    private val secureTcpClient: TcpClient
) : WebSocketClientConnectionManager {

    override fun connect(request: WebSocketClientRequest): CompletableFuture<WebSocketConnection> {
        Assert.hasText(request.url, "The websocket url must be not blank")
        Assert.notNull(request.policy, "The websocket policy must be not null")
        Assert.notNull(request.handler, "The websocket message handler must be not null")
        Assert.isTrue(request.policy.behavior == WebSocketBehavior.CLIENT, "The websocket behavior must be client")

        val uri = HttpURI(request.url)
        val tcpConnection = when (uri.scheme) {
            "ws" -> tcpClient.connect(uri.host, uri.port)
            "wss" -> secureTcpClient.connect(uri.host, uri.port)
            else -> throw WebSocketException("The websocket scheme error. scheme: ${uri.scheme}")
        }

        return tcpConnection.thenCompose { connection ->
            val httpConnection = Http1ClientConnection(config, connection)
            httpConnection.upgradeWebSocket(request)
        }
    }

}