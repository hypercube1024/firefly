package com.fireflysource.net.websocket.server.impl

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.server.HttpServer
import com.fireflysource.net.websocket.common.WebSocketConnection
import com.fireflysource.net.websocket.common.WebSocketMessageHandler
import com.fireflysource.net.websocket.common.impl.AsyncWebSocketConnection.Companion.defaultExtensionFactory
import com.fireflysource.net.websocket.common.model.ExtensionConfig
import com.fireflysource.net.websocket.common.model.WebSocketBehavior
import com.fireflysource.net.websocket.common.model.WebSocketPolicy
import com.fireflysource.net.websocket.server.*
import kotlinx.coroutines.launch

/**
 * @author Pengtao Qiu
 */
class AsyncWebSocketServerConnectionBuilder(
    private val httpServer: HttpServer,
    private val webSocketManager: WebSocketManager
) : WebSocketServerConnectionBuilder {

    private val connectionHandler = WebSocketServerConnectionHandler()

    override fun url(url: String): WebSocketServerConnectionBuilder {
        connectionHandler.url = url
        return this
    }

    override fun policy(policy: WebSocketPolicy): WebSocketServerConnectionBuilder {
        connectionHandler.policy = policy
        return this
    }

    override fun onExtensionSelect(selector: ExtensionSelector): WebSocketServerConnectionBuilder {
        connectionHandler.extensionSelector = selector
        return this
    }

    override fun onSubProtocolSelect(selector: SubProtocolSelector): WebSocketServerConnectionBuilder {
        connectionHandler.subProtocolSelector = selector
        return this
    }

    override fun onMessage(handler: WebSocketMessageHandler): WebSocketServerConnectionBuilder {
        connectionHandler.messageHandler = handler
        return this
    }

    override fun onAccept(listener: WebSocketServerConnectionListener): HttpServer {
        connectionHandler.connectionListener = listener
        if (connectionHandler.policy == null) {
            connectionHandler.policy = WebSocketPolicy(WebSocketBehavior.SERVER)
        }
        if (connectionHandler.subProtocolSelector == null) {
            connectionHandler.setSubProtocolSelector { listOf() }
        }
        if (connectionHandler.extensionSelector == null) {
            connectionHandler.setExtensionSelector { clientExtensions ->
                if (clientExtensions.isNullOrEmpty()) {
                    listOf()
                } else {
                    ExtensionConfig
                        .parseList(clientExtensions)
                        .filter { c -> defaultExtensionFactory.isAvailable(c.name) }
                        .map { c -> c.name }
                }
            }
        }
        webSocketManager.register(connectionHandler)
        return httpServer
    }
}

fun WebSocketServerConnectionBuilder.onAcceptAsync(block: suspend (WebSocketConnection) -> Unit): HttpServer {
    return this.onAccept { connection ->
        connection.coroutineScope.launch { block(connection) }
        Result.DONE
    }
}