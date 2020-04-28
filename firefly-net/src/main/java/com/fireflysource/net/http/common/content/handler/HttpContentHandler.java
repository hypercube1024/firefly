package com.fireflysource.net.http.common.content.handler;

import com.fireflysource.common.io.AsyncCloseable;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public interface HttpContentHandler<T> extends BiConsumer<ByteBuffer, T>, AsyncCloseable {
}
