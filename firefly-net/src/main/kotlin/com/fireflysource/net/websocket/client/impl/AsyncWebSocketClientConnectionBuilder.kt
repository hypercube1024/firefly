package com.fireflysource.net.websocket.client.impl

import com.fireflysource.net.websocket.client.WebSocketClientConnectionBuilder
import com.fireflysource.net.websocket.client.WebSocketClientConnectionManager
import com.fireflysource.net.websocket.client.WebSocketClientRequest
import com.fireflysource.net.websocket.common.WebSocketConnection
import com.fireflysource.net.websocket.common.WebSocketMessageHandler
import com.fireflysource.net.websocket.common.model.WebSocketBehavior
import com.fireflysource.net.websocket.common.model.WebSocketPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

/**
 * @author Pengtao Qiu
 */
class AsyncWebSocketClientConnectionBuilder(
    private val connectionManager: WebSocketClientConnectionManager
) : WebSocketClientConnectionBuilder {

    private val request = WebSocketClientRequest()

    override fun url(url: String): WebSocketClientConnectionBuilder {
        request.url = url
        return this
    }

    override fun policy(policy: WebSocketPolicy): WebSocketClientConnectionBuilder {
        request.policy = policy
        return this
    }

    override fun extensions(extensions: List<String>): WebSocketClientConnectionBuilder {
        request.extensions = extensions
        return this
    }

    override fun subProtocols(subProtocols: List<String>): WebSocketClientConnectionBuilder {
        request.subProtocols = subProtocols
        return this
    }

    override fun onMessage(handler: WebSocketMessageHandler): WebSocketClientConnectionBuilder {
        request.handler = handler
        return this
    }

    override fun connect(): CompletableFuture<WebSocketConnection> {
        if (request.policy == null) {
            request.policy = WebSocketPolicy(WebSocketBehavior.CLIENT)
        }
        if (request.subProtocols == null) {
            request.subProtocols = listOf()
        }
        if (request.extensions == null) {
            request.extensions = listOf()
        }
        return connectionManager.connect(request)
    }

}

fun WebSocketClientConnectionBuilder.connectAsync(block: suspend CoroutineScope.(WebSocketConnection) -> Unit) {
    this.connect().thenAccept { connection -> connection.coroutineScope.launch { block(connection) } }
}