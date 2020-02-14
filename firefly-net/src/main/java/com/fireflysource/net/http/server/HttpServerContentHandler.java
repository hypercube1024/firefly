package com.fireflysource.net.http.server;

import com.fireflysource.common.io.AsyncCloseable;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public interface HttpServerContentHandler extends BiConsumer<ByteBuffer, RoutingContext>, AsyncCloseable {
}
