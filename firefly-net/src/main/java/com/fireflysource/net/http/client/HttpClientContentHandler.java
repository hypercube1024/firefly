package com.fireflysource.net.http.client;

import com.fireflysource.common.io.AsyncCloseable;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

/**
 * @author Pengtao Qiu
 */
public interface HttpClientContentHandler extends BiConsumer<ByteBuffer, HttpClientResponse>, AsyncCloseable {

}
