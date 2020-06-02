package com.fireflysource.net.websocket.client;

import com.fireflysource.net.websocket.common.WebSocketConnection;
import com.fireflysource.net.websocket.common.WebSocketMessageHandler;
import com.fireflysource.net.websocket.common.model.WebSocketPolicy;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WebSocketClientConnectionBuilder {

    /**
     * Set the websocket url.
     *
     * @param url The websocket url.
     * @return The websocket client connection builder.
     */
    WebSocketClientConnectionBuilder url(URL url);

    /**
     * Set the websocket policy.
     *
     * @param policy The websocket policy.
     * @return The websocket client connection builder.
     */
    WebSocketClientConnectionBuilder policy(WebSocketPolicy policy);

    /**
     * Put the websocket extensions.
     *
     * @param extensions The websocket extensions.
     * @return The websocket client connection builder.
     */
    WebSocketClientConnectionBuilder putExtensions(List<String> extensions);

    /**
     * Add the websocket extension.
     *
     * @param extension The websocket extension.
     * @return The websocket client connection builder.
     */
    WebSocketClientConnectionBuilder addExtension(String extension);

    /**
     * Put the websocket sub protocols.
     *
     * @param subProtocols The websocket sub protocols.
     * @return The websocket client connection builder.
     */
    WebSocketClientConnectionBuilder putSubProtocols(List<String> subProtocols);

    /**
     * Set the websocket message handler.
     *
     * @param handler The websocket handler.
     * @return The websocket client connection builder.
     */
    WebSocketClientConnectionBuilder onMessage(WebSocketMessageHandler handler);

    /**
     * Create the websocket connection.
     *
     * @return The future websocket connection.
     */
    CompletableFuture<WebSocketConnection> connect();
}
