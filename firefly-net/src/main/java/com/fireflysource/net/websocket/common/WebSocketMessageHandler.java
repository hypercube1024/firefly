package com.fireflysource.net.websocket.common;

import com.fireflysource.net.websocket.common.frame.Frame;

import java.util.concurrent.CompletableFuture;

/**
 * The websocket message handler.
 *
 * @author Pengtao Qiu
 */
public interface WebSocketMessageHandler {

    CompletableFuture<Void> handle(Frame frame, WebSocketConnection connection);

}
