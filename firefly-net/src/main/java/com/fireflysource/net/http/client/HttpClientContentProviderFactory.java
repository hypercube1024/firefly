package com.fireflysource.net.http.client;

import com.fireflysource.net.http.client.impl.content.provider.ByteBufferContentProvider;
import com.fireflysource.net.http.client.impl.content.provider.FileContentProvider;
import com.fireflysource.net.http.client.impl.content.provider.StringContentProvider;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

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

    public static HttpClientContentProvider fileBody(Path path, Set<OpenOption> openOptions, long position, long length) {
        return new FileContentProvider(path, openOptions, position, length);
    }

    public static HttpClientContentProvider resourceFileBody(String resourcePath, OpenOption... openOptions) {
        URL resource = FileContentProvider.class.getClassLoader().getResource(resourcePath);
        try {
            if (resource != null) {
                URI uri = resource.toURI();
                return fileBody(Paths.get(uri), openOptions);
            } else {
                return null;
            }
        } catch (URISyntaxException e) {
            return null;
        }
    }

}
