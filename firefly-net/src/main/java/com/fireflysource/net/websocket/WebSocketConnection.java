package com.fireflysource.net.websocket;

import com.fireflysource.net.Connection;
import com.fireflysource.net.tcp.TcpCoroutineDispatcher;
import com.fireflysource.net.websocket.model.WebSocketPolicy;
import com.fireflysource.net.websocket.stream.IOState;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WebSocketConnection extends Connection, TcpCoroutineDispatcher {

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
    CompletableFuture<Void> sendText(String text);

    /**
     * Send binary message.
     *
     * @param data The binary message.
     * @return The future result.
     */
    CompletableFuture<Void> sendData(ByteBuffer data);

}
