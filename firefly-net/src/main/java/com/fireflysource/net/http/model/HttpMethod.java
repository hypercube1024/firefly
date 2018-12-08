package com.fireflysource.net.http.model;

import com.fireflysource.common.string.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public enum HttpMethod {
    GET,
    POST,
    HEAD,
    PUT,
    OPTIONS,
    DELETE,
    TRACE,
    CONNECT,
    MOVE,
    PROXY,
    PRI;

    private final String value;
    private final byte[] bytes;

    HttpMethod() {
        value = this.name();
        bytes = StringUtils.getUtf8Bytes(value);
        Holder.cache.put(value, this);
    }

    public static HttpMethod from(String value) {
        return Holder.cache.get(value);
    }

    public boolean is(String value) {
        return this.value.equalsIgnoreCase(value);
    }

    public String getValue() {
        return value;
    }

    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return value;
    }

    private static class Holder {
        private static final Map<String, HttpMethod> cache = new HashMap<>();
    }
}
