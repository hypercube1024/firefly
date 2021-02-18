package com.fireflysource.net.websocket.client;

import com.fireflysource.net.websocket.common.WebSocketConnection;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WebSocketClientConnectionManager {

    /**
     * Create a websocket connection.
     *
     * @param request The websocket connection request.
     * @return The websocket connection future.
     */
    CompletableFuture<WebSocketConnection> connect(WebSocketClientRequest request);

}
