package com.firefly.codec.websocket.stream;

import com.firefly.codec.common.ConnectionExtInfo;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.websocket.model.ExtensionConfig;
import com.firefly.codec.websocket.model.OutgoingFrames;
import com.firefly.net.Connection;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface WebSocketConnection extends OutgoingFrames, Connection, ConnectionExtInfo {

    /**
     * Register the connection close callback.
     *
     * @param closedListener The connection close callback.
     * @return The WebSocket connection.
     */
    WebSocketConnection onClose(Action1<WebSocketConnection> closedListener);

    /**
     * Register the exception callback.
     *
     * @param exceptionListener The exception callback.
     * @return The WebSocket connection.
     */
    WebSocketConnection onException(Action2<WebSocketConnection, Throwable> exceptionListener);

    /**
     * Get the read/write idle timeout.
     *
     * @return the idle timeout in milliseconds
     */
    long getIdleTimeout();

    /**
     * Get the IOState of the connection.
     *
     * @return the IOState of the connection.
     */
    IOState getIOState();

    /**
     * The policy that the connection is running under.
     *
     * @return the policy for the connection
     */
    WebSocketPolicy getPolicy();

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
    CompletableFuture<Boolean> sendText(String text);

    /**
     * Send binary message.
     *
     * @param data The binary message.
     * @return The future result.
     */
    CompletableFuture<Boolean> sendData(byte[] data);

    /**
     * Send binary message.
     *
     * @param data The binary message.
     * @return The future result.
     */
    CompletableFuture<Boolean> sendData(ByteBuffer data);

    /**
     * Get the websocket upgrade request.
     *
     * @return The upgrade request.
     */
    MetaData.Request getUpgradeRequest();

    /**
     * Get the websocket upgrade response.
     *
     * @return The upgrade response.
     */
    MetaData.Response getUpgradeResponse();

    /**
     * Get the extension config list.
     *
     * @return The extension config list.
     */
    List<ExtensionConfig> getExtensionConfigs();

}
