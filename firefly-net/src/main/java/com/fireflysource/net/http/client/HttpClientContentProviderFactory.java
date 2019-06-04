package com.fireflysource.net.http.client;

import com.fireflysource.net.http.client.impl.content.provider.ByteBufferContentProvider;
import com.fireflysource.net.http.client.impl.content.provider.FileContentProvider;
import com.fireflysource.net.http.client.impl.content.provider.StringContentProvider;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.Path;

abstract public class HttpClientContentProviderFactory {

    public static HttpClientContentProvider createByteBufferContentProvider(ByteBuffer buffer) {
        return new ByteBufferContentProvider(buffer);
    }

    public static HttpClientContentProvider createStringContentProvider(String string, Charset charset) {
        return new StringContentProvider(string, charset);
    }

    public static HttpClientContentProvider createFileContentProvider(Path path, OpenOption... openOptions) {
        return new FileContentProvider(path, openOptions);
    }

}
