package com.fireflysource.net.websocket.server

import com.fireflysource.common.`object`.Assert

/**
 * @author Pengtao Qiu
 */
class AsyncWebSocketManager : WebSocketManager {

    private val webSocketHandlers: MutableMap<String, WebSocketServerConnectionHandler> = HashMap()

    override fun register(connectionHandler: WebSocketServerConnectionHandler) {
        Assert.notNull(connectionHandler.url, "The websocket url must be not null")
        Assert.notNull(connectionHandler.extensionSelector, "The websocket extension selector must be not null")
        Assert.notNull(connectionHandler.subProtocolSelector, "The websocket sub protocol selector must be not null")
        Assert.notNull(connectionHandler.policy, "The websocket policy must be not null")
        Assert.notNull(connectionHandler.connectionListener, "The websocket connection listener must be not null")
        Assert.notNull(connectionHandler.messageHandler, "The websocket message handler must be not null")

        webSocketHandlers[connectionHandler.url] = connectionHandler
    }

    override fun findWebSocketHandler(path: String): WebSocketServerConnectionHandler? {
        return webSocketHandlers[path]
    }
}