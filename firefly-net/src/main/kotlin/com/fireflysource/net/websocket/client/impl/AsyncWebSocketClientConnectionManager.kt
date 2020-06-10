package com.fireflysource.net.websocket.client.impl

import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.websocket.client.WebSocketClientConnectionManager
import com.fireflysource.net.websocket.client.WebSocketClientRequest
import com.fireflysource.net.websocket.common.WebSocketConnection
import java.util.concurrent.CompletableFuture

/**
 * @author Pengtao Qiu
 */
class AsyncWebSocketClientConnectionManager(
    private val tcpClient: TcpClient,
    private val secureTcpClient: TcpClient
) : WebSocketClientConnectionManager {

    override fun connect(request: WebSocketClientRequest): CompletableFuture<WebSocketConnection> {
        TODO("Not yet implemented")
    }
}