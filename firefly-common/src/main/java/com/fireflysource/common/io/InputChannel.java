package com.fireflysource.common.io;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannel;
import java.util.concurrent.CompletableFuture;

public interface InputChannel extends AsynchronousChannel {

    /**
     * Read the content asynchronously.
     *
     * @param byteBuffer The buffer into which bytes are to be transferred.
     * @return The number of bytes read. If return -1, it presents the end of the content.
     */
    CompletableFuture<Integer> read(ByteBuffer byteBuffer);

}
