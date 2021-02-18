package com.fireflysource.net.http.client;

import com.fireflysource.net.http.client.impl.content.handler.ByteBufferContentHandler;
import com.fireflysource.net.http.client.impl.content.handler.FileContentHandler;
import com.fireflysource.net.http.client.impl.content.handler.StringContentHandler;

import java.nio.file.OpenOption;
import java.nio.file.Path;

abstract public class HttpClientContentHandlerFactory {

    public static HttpClientContentHandler bytesHandler() {
        return new ByteBufferContentHandler();
    }

    public static HttpClientContentHandler bytesHandler(long maxRequestBodySize) {
        return new ByteBufferContentHandler(maxRequestBodySize);
    }

    public static HttpClientContentHandler stringHandler() {
        return new StringContentHandler();
    }

    public static HttpClientContentHandler stringHandler(long maxRequestBodySize) {
        return new StringContentHandler(maxRequestBodySize);
    }

    public static HttpClientContentHandler fileHandler(Path path, OpenOption... openOptions) {
        return new FileContentHandler(path, openOptions);
    }
}
