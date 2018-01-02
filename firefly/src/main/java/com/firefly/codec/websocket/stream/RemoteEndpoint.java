package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.model.BatchMode;
import com.firefly.codec.websocket.model.WriteCallback;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

public interface RemoteEndpoint {
    /**
     * Send a binary message, returning when all bytes of the message has been transmitted.
     * <p>
     * Note: this is a blocking call
     *
     * @param data the message to be sent
     * @throws IOException if unable to send the bytes
     */
    void sendBytes(ByteBuffer data) throws IOException;

    /**
     * Initiates the asynchronous transmission of a binary message. This method returns before the message is
     * transmitted. Developers may use the returned
     * Future object to track progress of the transmission.
     *
     * @param data the data being sent
     * @return the Future object representing the send operation.
     */
    Future<Void> sendBytesByFuture(ByteBuffer data);

    /**
     * Initiates the asynchronous transmission of a binary message. This method returns before the message is
     * transmitted. Developers may provide a callback to
     * be notified when the message has been transmitted or resulted in an error.
     *
     * @param data     the data being sent
     * @param callback callback to notify of success or failure of the write operation
     */
    void sendBytes(ByteBuffer data, WriteCallback callback);

    /**
     * Send a binary message in pieces, blocking until all of the message has been transmitted. The runtime reads the
     * message in order. Non-final pieces are
     * sent with isLast set to false. The final piece must be sent with isLast set to true.
     *
     * @param fragment the piece of the message being sent
     * @param isLast   true if this is the last piece of the partial bytes
     * @throws IOException if unable to send the partial bytes
     */
    void sendPartialBytes(ByteBuffer fragment, boolean isLast) throws IOException;

    /**
     * Send a text message in pieces, blocking until all of the message has been transmitted. The runtime reads the
     * message in order. Non-final pieces are sent
     * with isLast set to false. The final piece must be sent with isLast set to true.
     *
     * @param fragment the piece of the message being sent
     * @param isLast   true if this is the last piece of the partial bytes
     * @throws IOException if unable to send the partial bytes
     */
    void sendPartialString(String fragment, boolean isLast) throws IOException;

    /**
     * Send a Ping message containing the given application data to the remote endpoint. The corresponding Pong message
     * may be picked up using the
     * MessageHandler.Pong handler.
     *
     * @param applicationData the data to be carried in the ping request
     * @throws IOException if unable to send the ping
     */
    void sendPing(ByteBuffer applicationData) throws IOException;

    /**
     * Allows the developer to send an unsolicited Pong message containing the given application data in order to serve
     * as a unidirectional heartbeat for the
     * session.
     *
     * @param applicationData the application data to be carried in the pong response.
     * @throws IOException if unable to send the pong
     */
    void sendPong(ByteBuffer applicationData) throws IOException;

    /**
     * Send a text message, blocking until all bytes of the message has been transmitted.
     * <p>
     * Note: this is a blocking call
     *
     * @param text the message to be sent
     * @throws IOException if unable to send the text message
     */
    void sendString(String text) throws IOException;

    /**
     * Initiates the asynchronous transmission of a text message. This method may return before the message is
     * transmitted. Developers may use the returned
     * Future object to track progress of the transmission.
     *
     * @param text the text being sent
     * @return the Future object representing the send operation.
     */
    Future<Void> sendStringByFuture(String text);

    /**
     * Initiates the asynchronous transmission of a text message. This method may return before the message is
     * transmitted. Developers may provide a callback to
     * be notified when the message has been transmitted or resulted in an error.
     *
     * @param text     the text being sent
     * @param callback callback to notify of success or failure of the write operation
     */
    void sendString(String text, WriteCallback callback);

    /**
     * @return the batch mode with which messages are sent.
     * @see #flush()
     */
    BatchMode getBatchMode();

    /**
     * Set the batch mode with which messages are sent.
     *
     * @param mode the batch mode to use
     * @see #flush()
     */
    void setBatchMode(BatchMode mode);

    /**
     * Get the InetSocketAddress for the established connection.
     *
     * @return the InetSocketAddress for the established connection. (or null, if the connection is no longer established)
     */
    InetSocketAddress getInetSocketAddress();

    /**
     * Flushes messages that may have been batched by the implementation.
     *
     * @throws IOException if the flush fails
     * @see #getBatchMode()
     */
    void flush() throws IOException;
}
