package com.fireflysource.net.io;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface InputChannel {

    /**
     * Read the content asynchronously.
     *
     * @param byteBuffer The buffer into which bytes are to be transferred.
     * @return The number of bytes read. If return -1, it presents the end of the content.
     */
    CompletableFuture<Integer> read(ByteBuffer byteBuffer);

}
