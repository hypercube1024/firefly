package com.fireflysource.net.websocket.common;

import com.fireflysource.net.Connection;
import com.fireflysource.net.http.common.model.MetaData;
import com.fireflysource.net.tcp.TcpCoroutineDispatcher;
import com.fireflysource.net.websocket.common.model.IncomingFrames;
import com.fireflysource.net.websocket.common.model.OutgoingFrames;
import com.fireflysource.net.websocket.common.model.WebSocketPolicy;
import com.fireflysource.net.websocket.common.stream.IOState;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WebSocketConnection extends Connection, TcpCoroutineDispatcher, OutgoingFrames {

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
    CompletableFuture<Void> sendText(String text);

    /**
     * Send binary message.
     *
     * @param data The binary message.
     * @return The future result.
     */
    CompletableFuture<Void> sendData(ByteBuffer data);

    /**
     * Set the next incoming frames.
     *
     * @param nextIncomingFrames The next incoming frames.
     */
    void setNextIncomingFrames(IncomingFrames nextIncomingFrames);

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
     * Begin to receive websocket data.
     */
    void begin();
}
