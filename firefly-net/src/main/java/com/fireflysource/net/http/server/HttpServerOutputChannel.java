package com.fireflysource.net.http.server;

import com.fireflysource.common.io.OutputChannel;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface HttpServerOutputChannel extends OutputChannel {

    /**
     * Commit the http response.
     *
     * @return The future result.
     */
    CompletableFuture<Void> commit();

    /**
     * If true, the http response has committed.
     *
     * @return If true, the http response has committed.
     */
    boolean isCommitted();

    /**
     * Write the message to the remote endpoint.
     *
     * @param byteBuffers The byte buffer array.
     * @param offset      The offset within the buffer array of the first buffer into which
     *                    bytes are to be transferred; must be non-negative and no larger than
     *                    byteBuffers.length.
     * @param length      The maximum number of buffers to be accessed; must be non-negative
     *                    and no larger than byteBuffers.length - offset.
     * @return The future result.
     */
    CompletableFuture<Long> write(ByteBuffer[] byteBuffers, int offset, int length);

    /**
     * Write the message to the remote endpoint.
     *
     * @param byteBufferList The byte buffer list.
     * @param offset         The offset within the buffer list of the first buffer into which
     *                       bytes are to be transferred; must be non-negative and no larger than
     *                       byteBufferList.length.
     * @param length         The maximum number of buffers to be accessed; must be non-negative
     *                       and no larger than byteBufferList.length - offset.
     * @return The future result.
     */
    CompletableFuture<Long> write(List<ByteBuffer> byteBufferList, int offset, int length);

    /**
     * Write the message to the remote endpoint.
     *
     * @param string The string.
     * @return The future result.
     */
    CompletableFuture<Integer> write(String string);

    /**
     * Write the message to the remote endpoint.
     *
     * @param string  The string.
     * @param charset The charset.
     * @return The future result.
     */
    CompletableFuture<Integer> write(String string, Charset charset);
}
