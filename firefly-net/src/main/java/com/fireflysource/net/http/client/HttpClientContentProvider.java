package com.fireflysource.net.http.client;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface HttpClientContentProvider {

    /**
     * Read the content asynchronously.
     *
     * @param byteBuffer The buffer into which bytes are to be transferred.
     * @return The number of bytes read. If return -1, it presents the end of the content.
     */
    CompletableFuture<Integer> read(ByteBuffer byteBuffer);

}
