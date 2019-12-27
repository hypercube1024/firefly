package com.fireflysource.net.tcp;

import com.fireflysource.common.func.Callback;
import com.fireflysource.common.sys.Result;
import com.fireflysource.net.Connection;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import kotlinx.coroutines.channels.Channel;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.fireflysource.common.sys.Result.futureToConsumer;

/**
 * The TCP connection. It reads or writes messages using the TCP (or TLS over the TCP) protocol.
 *
 * @author Pengtao Qiu
 */
public interface TcpConnection extends Connection, ApplicationProtocolSelector, TcpCoroutineDispatcher {

    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * If you start automatic message reading, the net framework sends the received the data to the message consumer callback automatically.
     *
     * @param messageConsumer Receiving message consumer.
     * @return The current connection.
     */
    TcpConnection onRead(Consumer<ByteBuffer> messageConsumer);

    /**
     * If you start automatic message reading, the net framework sends the received the data to the message consumer callback automatically.
     * The default message consumer will send data to the channel.
     *
     * @return The receiving data channel.
     */
    Channel<ByteBuffer> getInputChannel();

    /**
     * Start automatic message reading.
     *
     * @return The current connection.
     */
    TcpConnection startReading();

    /**
     * If return true, the tcp connection is reading messages.
     *
     * @return If return true, the tcp connection is reading messages.
     */
    boolean isReading();

    /**
     * Suspend automatic reading messages.
     *
     * @return The current connection.
     */
    TcpConnection suspendReading();

    /**
     * Register a connection close event callback. When the connection close, the framework will invoke this function.
     *
     * @param callback The connection close event callback.
     * @return The current connection.
     */
    TcpConnection onClose(Callback callback);

    /**
     * If the automatic message reading callback throws the exception, the framework will call this function.
     *
     * @param exception If the automatic message reading callback throws the exception, the framework will call this function.
     * @return The current connection.
     */
    TcpConnection onException(Consumer<Throwable> exception);

    /**
     * Close the current connection and wait the remaining messages of the channel have been sent completely.
     *
     * @param result When the connection close, the framework will invoke this function.
     * @return The current connection.
     */
    TcpConnection close(Consumer<Result<Void>> result);

    /**
     * Close the current connection immediately. The remaining messages of the channel will not be sent.
     *
     * @return The current connection.
     */
    TcpConnection closeNow();

    /**
     * If return true, the connection input channel has been closed. You can't receive any messages from the remote endpoint.
     *
     * @return If return true, the connection input channel has been closed.
     */
    boolean isShutdownInput();

    /**
     * If return true, the connection output channel has been closed. You can't send any messages to the remote endpoint.
     *
     * @return If return true, the connection output channel has been closed. You can't send any messages to the remote endpoint.
     */
    boolean isShutdownOutput();

    /**
     * Shutdown the connection for reading without closing the connection.
     *
     * @return The current connection.
     */
    TcpConnection shutdownInput();

    /**
     * Shutdown the connection for writing without closing the connection.
     *
     * @return The current connection.
     */
    TcpConnection shutdownOutput();

    /**
     * Write the message to the remote endpoint.
     *
     * @param byteBuffer The byte buffer.
     * @param result     The handler for consuming the result.
     * @return The current connection.
     */
    TcpConnection write(ByteBuffer byteBuffer, Consumer<Result<Integer>> result);

    /**
     * Write the message to the remote endpoint.
     *
     * @param byteBuffers The byte buffer array.
     * @param offset      The offset within the buffer array of the first buffer into which
     *                    bytes are to be transferred; must be non-negative and no larger than
     *                    byteBuffers.length.
     * @param length      The maximum number of buffers to be accessed; must be non-negative
     *                    and no larger than byteBuffers.length - offset.
     * @param result      The handler for consuming the result.
     * @return The current connection.
     */
    TcpConnection write(ByteBuffer[] byteBuffers, int offset, int length, Consumer<Result<Long>> result);

    /**
     * Write the message to the remote endpoint.
     *
     * @param byteBufferList The byte buffer list.
     * @param offset         The offset within the buffer array of the first buffer into which
     *                       bytes are to be transferred; must be non-negative and no larger than
     *                       byteBufferList.length.
     * @param length         The maximum number of buffers to be accessed; must be non-negative
     *                       and no larger than byteBufferList.length - offset.
     * @param result         The handler for consuming the result.
     * @return The current connection.
     */
    TcpConnection write(List<ByteBuffer> byteBufferList, int offset, int length, Consumer<Result<Long>> result);

    /**
     * Write the message to the remote endpoint.
     *
     * @param byteBuffer The byte buffer.
     * @return The future for consuming the result.
     */
    default CompletableFuture<Integer> write(ByteBuffer byteBuffer) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        write(byteBuffer, futureToConsumer(future));
        return future;
    }

    /**
     * Write the message to the remote endpoint.
     *
     * @param byteBuffers The byte buffer array.
     * @param offset      The offset within the buffer array of the first buffer into which
     *                    bytes are to be transferred; must be non-negative and no larger than
     *                    byteBuffers.length.
     * @param length      The maximum number of buffers to be accessed; must be non-negative
     *                    and no larger than byteBuffers.length - offset.
     * @return The future for consuming the result.
     */
    default CompletableFuture<Long> write(ByteBuffer[] byteBuffers, int offset, int length) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        write(byteBuffers, offset, length, futureToConsumer(future));
        return future;
    }

    /**
     * Write the message to the remote endpoint.
     *
     * @param byteBufferList The byte buffer list.
     * @param offset         The offset within the buffer array of the first buffer into which
     *                       bytes are to be transferred; must be non-negative and no larger than
     *                       byteBufferList.length.
     * @param length         The maximum number of buffers to be accessed; must be non-negative
     *                       and no larger than byteBufferList.length - offset.
     * @return The future for consuming the result.
     */
    default CompletableFuture<Long> write(List<ByteBuffer> byteBufferList, int offset, int length) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        write(byteBufferList, offset, length, futureToConsumer(future));
        return future;
    }

    /**
     * Write the message to the remote endpoint.
     *
     * @param bytes  The byte array.
     * @param result The handler for consuming the result.
     * @return The current connection.
     */
    default TcpConnection write(byte[] bytes, Consumer<Result<Integer>> result) {
        return write(ByteBuffer.wrap(bytes), result);
    }

    /**
     * Write the message to the remote endpoint.
     *
     * @param string The string.
     * @param result The handler for consuming the result.
     * @return The current connection.
     */
    default TcpConnection write(String string, Consumer<Result<Integer>> result) {
        return write(ByteBuffer.wrap(string.getBytes(DEFAULT_CHARSET)), result);
    }

    /**
     * If you enable the TLS protocol, it returns true.
     *
     * @return If you enable the TLS protocol, it returns true.
     */
    boolean isSecureConnection();

    /**
     * If you enable the TLS protocol, it presents the TLS engine is client mode or server mode.
     *
     * @return The TLS engine is client mode or server mode.
     */
    boolean isClientMode();

    /**
     * If return true, the TLS engine completes the handshake stage.
     *
     * @return If return true, the TLS engine completes the handshake stage.
     */
    boolean isHandshakeComplete();

    /**
     * Listen the TLS handshake complete event. If the TLS handshake has finished, the framework will invoke the callback function.
     *
     * @param result The value is the negotiated application layer protocol.
     * @return The current connection.
     */
    TcpConnection onHandshakeComplete(Consumer<Result<String>> result);

    /**
     * Listen the TLS handshake complete event.
     *
     * @return The value is the negotiated application layer protocol.
     */
    default CompletableFuture<String> onHandshakeComplete() {
        CompletableFuture<String> future = new CompletableFuture<>();
        onHandshakeComplete(futureToConsumer(future));
        return future;
    }

}
