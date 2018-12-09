package com.fireflysource.net.http.model;

import com.fireflysource.common.collection.trie.ArrayTrie;
import com.fireflysource.common.collection.trie.Trie;
import com.fireflysource.common.string.StringUtils;

import java.nio.ByteBuffer;

public enum HttpVersion {
    HTTP_0_9("HTTP/0.9", 9),
    HTTP_1_0("HTTP/1.0", 10),
    HTTP_1_1("HTTP/1.1", 11),
    HTTP_2("HTTP/2.0", 20);

    public final static Trie<HttpVersion> CACHE = new ArrayTrie<>();

    static {
        for (HttpVersion version : HttpVersion.values())
            CACHE.put(version.toString(), version);
    }

    private final String value;
    private final int version;
    private final byte[] bytes;

    HttpVersion(String value, int version) {
        this.value = value;
        this.version = version;
        bytes = StringUtils.getUtf8Bytes(value);
    }

    public static HttpVersion from(String value) {
        return CACHE.get(value);
    }

    /**
     * Optimised lookup to find a Http Version and whitespace in a byte array.
     *
     * @param bytes    Array containing ISO-8859-1 characters
     * @param position The first valid index
     * @param limit    The first non valid index
     * @return A HttpMethod if a match or null if no easy match.
     */
    public static HttpVersion lookAheadGet(byte[] bytes, int position, int limit) {
        int length = limit - position;
        if (length < 9)
            return null;

        if (bytes[position + 4] == '/' && bytes[position + 6] == '.' && Character.isWhitespace((char) bytes[position + 8]) &&
                ((bytes[position] == 'H' && bytes[position + 1] == 'T' && bytes[position + 2] == 'T' && bytes[position + 3] == 'P') ||
                        (bytes[position] == 'h' && bytes[position + 1] == 't' && bytes[position + 2] == 't' && bytes[position + 3] == 'p'))) {
            switch (bytes[position + 5]) {
                case '1':
                    switch (bytes[position + 7]) {
                        case '0':
                            return HTTP_1_0;
                        case '1':
                            return HTTP_1_1;
                    }
                    break;
                case '2':
                    switch (bytes[position + 7]) {
                        case '0':
                            return HTTP_2;
                    }
                    break;
            }
        }

        return null;
    }

    /**
     * Optimised lookup to find a HTTP Version and trailing white space in a byte array.
     *
     * @param buffer buffer containing ISO-8859-1 characters
     * @return A HttpVersion if a match or null if no easy match.
     */
    public static HttpVersion lookAheadGet(ByteBuffer buffer) {
        if (buffer.hasArray())
            return lookAheadGet(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.arrayOffset() + buffer.limit());
        return null;
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

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return value;
    }
}
