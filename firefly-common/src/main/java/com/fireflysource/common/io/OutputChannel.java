package com.fireflysource.common.io;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannel;
import java.util.concurrent.CompletableFuture;

public interface OutputChannel extends AsynchronousChannel, AsyncCloseable {

    /**
     * Write the content asynchronously.
     *
     * @param byteBuffer The buffer into which bytes are to be transferred.
     * @return The number of bytes write. If return -1, it presents the end of the content.
     */
    CompletableFuture<Integer> write(ByteBuffer byteBuffer);

}
