package com.fireflysource.net.http.client;

import com.fireflysource.net.http.client.impl.content.provider.ByteBufferContentProvider;
import com.fireflysource.net.http.client.impl.content.provider.FileContentProvider;
import com.fireflysource.net.http.client.impl.content.provider.StringContentProvider;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;

abstract public class HttpClientContentProviderFactory {

    public static HttpClientContentProvider bytesBody(ByteBuffer buffer) {
        return new ByteBufferContentProvider(buffer);
    }

    public static HttpClientContentProvider stringBody(String string) {
        return new StringContentProvider(string, StandardCharsets.UTF_8);
    }

    public static HttpClientContentProvider stringBody(String string, Charset charset) {
        return new StringContentProvider(string, charset);
    }

    public static HttpClientContentProvider fileBody(Path path, OpenOption... openOptions) {
        return new FileContentProvider(path, openOptions);
    }

}
