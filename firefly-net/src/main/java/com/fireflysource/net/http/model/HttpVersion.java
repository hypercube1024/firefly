package com.fireflysource.net.http.model;


import com.fireflysource.common.string.StringUtils;

import java.util.HashMap;
import java.util.Map;

public enum HttpVersion {
    HTTP_1_0("HTTP/1.0"),
    HTTP_1_1("HTTP/1.1"),
    HTTP_2("HTTP/2.0");

    private final String value;
    private final byte[] bytes;

    HttpVersion(String value) {
        this.value = value;
        bytes = StringUtils.getUtf8Bytes(value);
        Holder.cache.put(value, this);
    }

    public static HttpVersion from(String value) {
        return Holder.cache.get(value);
    }

    public String getValue() {
        return value;
    }

    public byte[] getBytes() {
        return bytes;
    }

    private static class Holder {
        private static final Map<String, HttpVersion> cache = new HashMap<>();
    }
}
