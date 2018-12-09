package com.fireflysource.net.http.common.model;

import com.fireflysource.common.collection.trie.ArrayTernaryTrie;
import com.fireflysource.common.collection.trie.ArrayTrie;
import com.fireflysource.common.collection.trie.Trie;
import com.fireflysource.common.string.StringUtils;

import java.nio.ByteBuffer;

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

    public final static Trie<HttpMethod> CACHE = new ArrayTernaryTrie<>(false);
    public final static Trie<HttpMethod> INSENSITIVE_CACHE = new ArrayTrie<>();

    static {
        for (HttpMethod method : HttpMethod.values())
            CACHE.put(method.toString(), method);
    }

    static {
        for (HttpMethod method : HttpMethod.values())
            INSENSITIVE_CACHE.put(method.toString(), method);
    }

    private final String value;
    private final byte[] bytes;

    HttpMethod() {
        value = this.name();
        bytes = StringUtils.getUtf8Bytes(value);
    }

    public static HttpMethod from(String value) {
        return CACHE.get(value);
    }

    /**
     * Optimized lookup to find a method name and trailing space in a byte array.
     *
     * @param bytes    Array containing ISO-8859-1 characters
     * @param position The first valid index
     * @param limit    The first non valid index
     * @return A HttpMethod if a match or null if no easy match.
     */
    public static HttpMethod lookAheadGet(byte[] bytes, final int position, int limit) {
        int length = limit - position;
        if (length < 4)
            return null;
        switch (bytes[position]) {
            case 'G':
                if (bytes[position + 1] == 'E' && bytes[position + 2] == 'T' && bytes[position + 3] == ' ')
                    return GET;
                break;
            case 'P':
                if (bytes[position + 1] == 'O' && bytes[position + 2] == 'S' && bytes[position + 3] == 'T' && length >= 5 && bytes[position + 4] == ' ')
                    return POST;
                if (bytes[position + 1] == 'R' && bytes[position + 2] == 'O' && bytes[position + 3] == 'X' && length >= 6 && bytes[position + 4] == 'Y' && bytes[position + 5] == ' ')
                    return PROXY;
                if (bytes[position + 1] == 'U' && bytes[position + 2] == 'T' && bytes[position + 3] == ' ')
                    return PUT;
                if (bytes[position + 1] == 'R' && bytes[position + 2] == 'I' && bytes[position + 3] == ' ')
                    return PRI;
                break;
            case 'H':
                if (bytes[position + 1] == 'E' && bytes[position + 2] == 'A' && bytes[position + 3] == 'D' && length >= 5 && bytes[position + 4] == ' ')
                    return HEAD;
                break;
            case 'O':
                if (bytes[position + 1] == 'P' && bytes[position + 2] == 'T' && bytes[position + 3] == 'I' && length >= 8 &&
                        bytes[position + 4] == 'O' && bytes[position + 5] == 'N' && bytes[position + 6] == 'S' && bytes[position + 7] == ' ')
                    return OPTIONS;
                break;
            case 'D':
                if (bytes[position + 1] == 'E' && bytes[position + 2] == 'L' && bytes[position + 3] == 'E' && length >= 7 &&
                        bytes[position + 4] == 'T' && bytes[position + 5] == 'E' && bytes[position + 6] == ' ')
                    return DELETE;
                break;
            case 'T':
                if (bytes[position + 1] == 'R' && bytes[position + 2] == 'A' && bytes[position + 3] == 'C' && length >= 6 &&
                        bytes[position + 4] == 'E' && bytes[position + 5] == ' ')
                    return TRACE;
                break;
            case 'C':
                if (bytes[position + 1] == 'O' && bytes[position + 2] == 'N' && bytes[position + 3] == 'N' && length >= 8 &&
                        bytes[position + 4] == 'E' && bytes[position + 5] == 'C' && bytes[position + 6] == 'T' && bytes[position + 7] == ' ')
                    return CONNECT;
                break;
            case 'M':
                if (bytes[position + 1] == 'O' && bytes[position + 2] == 'V' && bytes[position + 3] == 'E' && length >= 5 && bytes[position + 4] == ' ')
                    return MOVE;
                break;

            default:
                break;
        }
        return null;
    }

    /**
     * Optimized lookup to find a method name and trailing space in a byte array.
     *
     * @param buffer buffer containing ISO-8859-1 characters, it is not modified.
     * @return A HttpMethod if a match or null if no easy match.
     */
    public static HttpMethod lookAheadGet(ByteBuffer buffer) {
        if (buffer.hasArray())
            return lookAheadGet(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.arrayOffset() + buffer.limit());

        int l = buffer.remaining();
        if (l >= 4) {
            HttpMethod m = CACHE.getBest(buffer, 0, l);
            if (m != null) {
                int ml = m.getValue().length();
                if (l > ml && buffer.get(buffer.position() + ml) == ' ')
                    return m;
            }
        }
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

    @Override
    public String toString() {
        return value;
    }

}
