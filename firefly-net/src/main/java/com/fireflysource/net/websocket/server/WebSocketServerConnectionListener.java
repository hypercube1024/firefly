package com.fireflysource.net.websocket.server;

import com.fireflysource.net.websocket.common.WebSocketConnection;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WebSocketServerConnectionListener {

    CompletableFuture<Void> accept(WebSocketConnection connection);

}
