package com.fireflysource.net.websocket.server;

/**
 * @author Pengtao Qiu
 */
public interface WebSocketManager extends Cloneable {

    /**
     * Find the websocket handler.
     *
     * @param path The request path.
     * @return The websocket handler.
     */
    WebSocketServerConnectionHandler findWebSocketHandler(String path);

    /**
     * Register the websocket handler.
     *
     * @param connectionHandler The websocket handler.
     */
    void register(WebSocketServerConnectionHandler connectionHandler);

}
