package com.fireflysource.net.websocket.server

import com.fireflysource.net.http.server.HttpServer
import com.fireflysource.net.websocket.common.WebSocketMessageHandler
import com.fireflysource.net.websocket.common.impl.AsyncWebSocketConnection.Companion.defaultExtensionFactory
import com.fireflysource.net.websocket.common.model.WebSocketBehavior
import com.fireflysource.net.websocket.common.model.WebSocketPolicy

/**
 * @author Pengtao Qiu
 */
class AsyncWebSocketServerConnectionBuilder(
    private val httpServer: HttpServer,
    private val webSocketManager: WebSocketManager
) : WebSocketServerConnectionBuilder {

    private val connectionHandler = WebSocketServerConnectionHandler()

    init {
        connectionHandler.setSubProtocolSelector { listOf() }
        connectionHandler.setExtensionSelector { clientExtensions ->
            if (clientExtensions.isNullOrEmpty()) {
                listOf()
            } else {
                val serverExtensionNames = defaultExtensionFactory.extensionNames
                clientExtensions.filter { name -> serverExtensionNames.contains(name) }
            }
        }
        connectionHandler.policy = WebSocketPolicy(WebSocketBehavior.SERVER)
    }

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
        webSocketManager.register(connectionHandler)
        return httpServer
    }
}