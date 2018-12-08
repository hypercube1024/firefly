package com.fireflysource.net.http.model;


import com.fireflysource.common.string.StringUtils;

import java.util.HashMap;
import java.util.Map;

public enum HttpScheme {
    HTTP("http"), HTTPS("https"), WS("ws"), WSS("wss");

    private final String value;
    private final byte[] bytes;

    HttpScheme(String value) {
        this.value = value;
        bytes = StringUtils.getUtf8Bytes(value);
        Holder.cache.put(value, this);
    }

    public static HttpScheme from(String value) {
        return Holder.cache.get(value);
    }

    public boolean is(String value) {
        return value != null && value.equalsIgnoreCase(this.value);
    }

    public String getValue() {
        return value;
    }

    public byte[] getBytes() {
        return bytes;
    }

    private static class Holder {
        private static final Map<String, HttpScheme> cache = new HashMap<>();
    }
}
