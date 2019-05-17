package com.fireflysource.common.io;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannel;
import java.util.concurrent.CompletableFuture;

public interface InputChannel extends AsynchronousChannel {

    int END_OF_STREAM_FLAG = -1;

    /**
     * Read the content asynchronously.
     *
     * @param byteBuffer The buffer into which bytes are to be transferred.
     * @return The number of bytes read. If return -1, it presents the end of the content.
     */
    CompletableFuture<Integer> read(ByteBuffer byteBuffer);

    /**
     * Return the end of stream flag future wrap.
     *
     * @return The end of stream flag.
     */
    default CompletableFuture<Integer> endStream() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        future.complete(END_OF_STREAM_FLAG);
        return future;
    }

}
