package com.fireflysource.net.websocket.common;

import com.fireflysource.net.Connection;
import com.fireflysource.net.tcp.TcpCoroutineDispatcher;
import com.fireflysource.net.websocket.common.extension.ExtensionFactory;
import com.fireflysource.net.websocket.common.frame.Frame;
import com.fireflysource.net.websocket.common.model.WebSocketPolicy;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The websocket connection.
 *
 * @author Pengtao Qiu
 */
public interface WebSocketConnection extends Connection, TcpCoroutineDispatcher, WebSocketConnectionState {

    /**
     * Get the absolute URL of the WebSocket.
     *
     * @return The absolute URL of the WebSocket.
     */
    String getUrl();

    /**
     * Get the websocket extensions.
     *
     * @return The websocket extensions.
     */
    List<String> getExtensions();

    /**
     * Get the websocket sub protocols.
     *
     * @return The websocket sub protocols.
     */
    List<String> getSubProtocols();

    /**
     * Get the policy that the connection is running under.
     *
     * @return the policy for the connection
     */
    WebSocketPolicy getPolicy();

    /**
     * Get the websocket extension factory.
     *
     * @return The websocket extension factory.
     */
    ExtensionFactory getExtensionFactory();

    /**
     * Generate random 4bytes mask key
     *
     * @return the mask key
     */
    byte[] generateMask();

    /**
     * Send text message.
     *
     * @param text The text message.
     * @return The future result.
     */
    CompletableFuture<Void> sendText(String text);

    /**
     * Send binary message.
     *
     * @param data The binary message.
     * @return The future result.
     */
    CompletableFuture<Void> sendData(ByteBuffer data);

    /**
     * Send websocket frame.
     *
     * @param frame The websocket frame.
     * @return The future result.
     */
    CompletableFuture<Void> sendFrame(Frame frame);

    /**
     * Set the websocket message handler.
     *
     * @param handler The websocket message handler.
     */
    void setWebSocketMessageHandler(WebSocketMessageHandler handler);

    /**
     * Begin to receive websocket data.
     */
    void begin();
}
