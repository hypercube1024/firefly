package com.fireflysource.net.http.client;

import com.fireflysource.net.http.common.content.provider.HttpContentProvider;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public interface HttpClientContentProvider extends HttpContentProvider {

    /**
     * The content length. If the length is -1, the content is the data stream.
     *
     * @return The content length.
     */
    long length();

    /**
     * Convert fixed length content to a ByteBuffer. If the content is the data stream, return an empty ByteBuffer.
     *
     * @return The ByteBuffer.
     */
    ByteBuffer toByteBuffer();

}
