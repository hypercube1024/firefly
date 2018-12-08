package com.fireflysource.net.http.model;

import com.fireflysource.common.collection.trie.ArrayTrie;
import com.fireflysource.common.collection.trie.Trie;
import com.fireflysource.common.string.StringUtils;

import java.util.EnumSet;

/**
 *
 */
public enum HttpHeaderValue {

    CLOSE("close"),
    CHUNKED("chunked"),
    GZIP("gzip"),
    IDENTITY("identity"),
    KEEP_ALIVE("keep-alive"),
    CONTINUE("100-continue"),
    PROCESSING("102-processing"),
    TE("TE"),
    BYTES("bytes"),
    NO_CACHE("no-cache"),
    UPGRADE("Upgrade"),
    UNKNOWN("::UNKNOWN::");

    public final static Trie<HttpHeaderValue> CACHE = new ArrayTrie<>();
    private static final EnumSet<HttpHeader> __known = EnumSet.of(
            HttpHeader.CONNECTION,
            HttpHeader.TRANSFER_ENCODING,
            HttpHeader.CONTENT_ENCODING);

    static {
        for (HttpHeaderValue value : HttpHeaderValue.values())
            if (value != UNKNOWN)
                CACHE.put(value.toString(), value);
    }

    private final String value;
    private final byte[] bytes;

    HttpHeaderValue(String value) {
        this.value = value;
        bytes = StringUtils.getUtf8Bytes(value);
    }

    public static boolean hasKnownValues(HttpHeader header) {
        if (header == null)
            return false;
        return __known.contains(header);
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
}
