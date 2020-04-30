package com.fireflysource.net.http.common.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum ContentEncoding {

    GZIP("gzip"),
    DEFLATE("deflate"),
    BR("br");

    private static class Holder {
        private static final Map<String, ContentEncoding> map = new HashMap<>(4);
    }

    private final String value;

    ContentEncoding(String value) {
        this.value = value;
        Holder.map.put(value, this);
    }

    public String getValue() {
        return value;
    }

    public static Optional<ContentEncoding> from(String value) {
        return Optional.ofNullable(Holder.map.get(value));
    }

}
