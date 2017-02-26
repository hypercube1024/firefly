package com.firefly.codec.http2.model;

public abstract class AbstractTypedContentProvider implements ContentProvider.Typed {
    private final String contentType;

    protected AbstractTypedContentProvider(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }
}
