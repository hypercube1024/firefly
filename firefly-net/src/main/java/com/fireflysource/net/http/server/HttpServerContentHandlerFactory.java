package com.fireflysource.net.http.server;


import com.fireflysource.net.http.server.impl.content.handler.ByteBufferContentHandler;
import com.fireflysource.net.http.server.impl.content.handler.FileContentHandler;
import com.fireflysource.net.http.server.impl.content.handler.StringContentHandler;

import java.nio.file.OpenOption;
import java.nio.file.Path;

abstract public class HttpServerContentHandlerFactory {
    public static HttpServerContentHandler bytesHandler() {
        return new ByteBufferContentHandler();
    }

    public static HttpServerContentHandler bytesHandler(long maxRequestBodySize) {
        return new ByteBufferContentHandler(maxRequestBodySize);
    }

    public static HttpServerContentHandler stringHandler() {
        return new StringContentHandler();
    }

    public static HttpServerContentHandler stringHandler(long maxRequestBodySize) {
        return new StringContentHandler(maxRequestBodySize);
    }

    public static HttpServerContentHandler fileHandler(Path path, OpenOption... openOptions) {
        return new FileContentHandler(path, openOptions);
    }
}
