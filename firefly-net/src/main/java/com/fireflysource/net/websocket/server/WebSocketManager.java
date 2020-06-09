package com.fireflysource.net.websocket.server;

/**
 * @author Pengtao Qiu
 */
public interface WebSocketManager {

    /**
     * Find the websocket handler.
     *
     * @param path The request path.
     * @return The websocket handler.
     */
    WebSocketServerConnectionHandler findWebSocketHandler(String path);

}
