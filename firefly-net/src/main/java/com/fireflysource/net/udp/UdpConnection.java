package com.fireflysource.net.udp;

import com.fireflysource.common.io.AsyncCloseable;
import com.fireflysource.common.sys.Result;
import com.fireflysource.net.Connection;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.fireflysource.common.sys.Result.futureToConsumer;

/**
 * The UDP connection. It reads or writes messages using the UDP (or DTLS over the UDP) protocol.
 */
public interface UdpConnection extends Connection, AsyncCloseable {

    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * Read data from the remote endpoint.
     *
     * @return The future data.
     */
    CompletableFuture<ByteBuffer> read();

    /**
     * Write the data to the remote endpoint.
     *
     * @param byteBuffer The byte buffer.
     * @param result     The handler for consuming the result.
     * @return The current connection.
     */
    UdpConnection write(ByteBuffer byteBuffer, Consumer<Result<Integer>> result);

    /**
     * Write the data to the remote endpoint.
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
    UdpConnection write(ByteBuffer[] byteBuffers, int offset, int length, Consumer<Result<Long>> result);

    /**
     * Write the data to the remote endpoint.
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
    UdpConnection write(List<ByteBuffer> byteBufferList, int offset, int length, Consumer<Result<Long>> result);

    /**
     * Write the data to the remote endpoint.
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
     * Write and flush data to the remote endpoint.
     *
     * @param byteBuffer The byte buffer.
     * @return The future for consuming the result.
     */
    default CompletableFuture<Integer> writeAndFlush(ByteBuffer byteBuffer) {
        return write(byteBuffer).thenCompose(len -> flush().thenApply(n -> len));
    }

    /**
     * Write the data to the remote endpoint.
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
     * Write the data to the remote endpoint.
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
     * Write and flush data to the remote endpoint.
     *
     * @param byteBufferList The byte buffer list.
     * @param offset         The offset within the buffer array of the first buffer into which
     *                       bytes are to be transferred; must be non-negative and no larger than
     *                       byteBufferList.length.
     * @param length         The maximum number of buffers to be accessed; must be non-negative
     *                       and no larger than byteBufferList.length - offset.
     * @return The future for consuming the result.
     */
    default CompletableFuture<Long> writeAndFlush(List<ByteBuffer> byteBufferList, int offset, int length) {
        return write(byteBufferList, offset, length).thenCompose(len -> flush().thenApply(n -> len));
    }

    /**
     * Write the data to the remote endpoint.
     *
     * @param bytes  The byte array.
     * @param result The handler for consuming the result.
     * @return The current connection.
     */
    default UdpConnection write(byte[] bytes, Consumer<Result<Integer>> result) {
        return write(ByteBuffer.wrap(bytes), result);
    }

    /**
     * Write the data to the remote endpoint.
     *
     * @param string The string.
     * @param result The handler for consuming the result.
     * @return The current connection.
     */
    default UdpConnection write(String string, Consumer<Result<Integer>> result) {
        return write(ByteBuffer.wrap(string.getBytes(DEFAULT_CHARSET)), result);
    }

    /**
     * Flush output buffer to remote endpoint.
     *
     * @param result When flush data to remote endpoint, the framework will invoke this function.
     * @return The current connection.
     */
    UdpConnection flush(Consumer<Result<Void>> result);

    /**
     * Flush output buffer to remote endpoint.
     *
     * @return The future result.
     */
    default CompletableFuture<Void> flush() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        flush(futureToConsumer(future));
        return future;
    }

    /**
     * Get output buffer size.
     *
     * @return The output buffer size.
     */
    int getBufferSize();
}
