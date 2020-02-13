package com.fireflysource.net.http.server;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface HttpServerContentHandler extends BiConsumer<ByteBuffer, RoutingContext> {
}
