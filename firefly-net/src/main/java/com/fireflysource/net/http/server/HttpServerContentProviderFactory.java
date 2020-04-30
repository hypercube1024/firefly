package com.fireflysource.net.http.server;

import com.fireflysource.net.http.common.codec.ContentEncoded;
import com.fireflysource.net.http.common.model.ContentEncoding;
import com.fireflysource.net.http.server.impl.content.provider.ByteBufferContentProvider;
import com.fireflysource.net.http.server.impl.content.provider.FileContentProvider;
import com.fireflysource.net.http.server.impl.content.provider.StringContentProvider;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Set;

abstract public class HttpServerContentProviderFactory {

    public static HttpServerContentProvider bytesBody(ByteBuffer buffer) {
        return new ByteBufferContentProvider(buffer);
    }

    public static HttpServerContentProvider stringBody(String string, Charset charset) {
        return new StringContentProvider(string, charset);
    }

    public static HttpServerContentProvider stringBody(String string, Charset charset, ContentEncoding encoding) {
        try {
            byte[] data = string.getBytes(charset);
            final byte[] encodedBytes = ContentEncoded.encode(data, encoding);
            return new ByteBufferContentProvider(ByteBuffer.wrap(encodedBytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static HttpServerContentProvider fileBody(Path path, OpenOption... openOptions) {
        return new FileContentProvider(path, openOptions);
    }

    public static HttpServerContentProvider fileBody(Path path, Set<OpenOption> openOptions, long position, long length) {
        return new FileContentProvider(path, openOptions, position, length);
    }
}
