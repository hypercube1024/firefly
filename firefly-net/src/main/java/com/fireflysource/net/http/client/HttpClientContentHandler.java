package com.fireflysource.net.http.client;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

/**
 * @author Pengtao Qiu
 */
@FunctionalInterface
public interface HttpClientContentHandler extends BiConsumer<ByteBuffer, HttpClientResponse> {

}
