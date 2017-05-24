package com.firefly.codec.http2.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A {@link ContentProvider} for strings.
 * <p>
 * It is possible to specify, at the constructor, an encoding used to convert
 * the string into bytes, by default UTF-8.
 */
public class StringContentProvider extends BytesContentProvider {
    public StringContentProvider(String content) {
        this(content, StandardCharsets.UTF_8);
    }

    public StringContentProvider(String content, String encoding) {
        this(content, Charset.forName(encoding));
    }

    public StringContentProvider(String content, Charset charset) {
        this("text/plain;charset=" + charset.name(), content, charset);
    }

    public StringContentProvider(String contentType, String content, Charset charset) {
        super(contentType, content.getBytes(charset));
    }
}
