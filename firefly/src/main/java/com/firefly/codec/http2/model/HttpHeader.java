package com.firefly.codec.http2.model;

import com.firefly.utils.StringUtils;
import com.firefly.utils.collection.ArrayTrie;
import com.firefly.utils.collection.Trie;

import java.nio.ByteBuffer;


public enum HttpHeader {

    /**
     * General Fields.
     */
    CONNECTION("Connection"),
    CACHE_CONTROL("Cache-Control"),
    DATE("Date"),
    PRAGMA("Pragma"),
    PROXY_CONNECTION("Proxy-Connection"),
    TRAILER("Trailer"),
    TRANSFER_ENCODING("Transfer-Encoding"),
    UPGRADE("Upgrade"),
    VIA("Via"),
    WARNING("Warning"),
    NEGOTIATE("Negotiate"),

    /**
     * Entity Fields.
     */
    ALLOW("Allow"),
    CONTENT_ENCODING("Content-Encoding"),
    CONTENT_LANGUAGE("Content-Language"),
    CONTENT_LENGTH("Content-Length"),
    CONTENT_LOCATION("Content-Location"),
    CONTENT_MD5("Content-MD5"),
    CONTENT_RANGE("Content-Range"),
    CONTENT_TYPE("Content-Type"),
    EXPIRES("Expires"),
    LAST_MODIFIED("Last-Modified"),

    /**
     * Request Fields.
     */
    ACCEPT("Accept"),
    ACCEPT_CHARSET("Accept-Charset"),
    ACCEPT_ENCODING("Accept-Encoding"),
    ACCEPT_LANGUAGE("Accept-Language"),
    AUTHORIZATION("Authorization"),
    EXPECT("Expect"),
    FORWARDED("Forwarded"),
    FROM("From"),
    HOST("Host"),
    IF_MATCH("If-Match"),
    IF_MODIFIED_SINCE("If-Modified-Since"),
    IF_NONE_MATCH("If-None-Match"),
    IF_RANGE("If-Range"),
    IF_UNMODIFIED_SINCE("If-Unmodified-Since"),
    KEEP_ALIVE("Keep-Alive"),
    MAX_FORWARDS("Max-Forwards"),
    PROXY_AUTHORIZATION("Proxy-Authorization"),
    RANGE("Range"),
    REQUEST_RANGE("Request-Range"),
    REFERER("Referer"),
    TE("TE"),
    USER_AGENT("User-Agent"),
    X_FORWARDED_FOR("X-Forwarded-For"),
    X_FORWARDED_PROTO("X-Forwarded-Proto"),
    X_FORWARDED_SERVER("X-Forwarded-Server"),
    X_FORWARDED_HOST("X-Forwarded-Host"),

    /**
     * Response Fields.
     */
    ACCEPT_RANGES("Accept-Ranges"),
    AGE("Age"),
    ETAG("ETag"),
    LOCATION("Location"),
    PROXY_AUTHENTICATE("Proxy-Authenticate"),
    RETRY_AFTER("Retry-After"),
    SERVER("Server"),
    SERVLET_ENGINE("Servlet-Engine"),
    VARY("Vary"),
    WWW_AUTHENTICATE("WWW-Authenticate"),

    /**
     * WebSocket Fields.
     */
    ORIGIN("Origin"),
    SEC_WEBSOCKET_KEY("Sec-WebSocket-Key"),
    SEC_WEBSOCKET_VERSION("Sec-WebSocket-Version"),
    SEC_WEBSOCKET_EXTENSIONS("Sec-WebSocket-Extensions"),
    SEC_WEBSOCKET_SUBPROTOCOL("Sec-WebSocket-Protocol"),
    SEC_WEBSOCKET_ACCEPT("Sec-WebSocket-Accept"),

    /**
     * Other Fields.
     */
    COOKIE("Cookie"),
    SET_COOKIE("Set-Cookie"),
    SET_COOKIE2("Set-Cookie2"),
    MIME_VERSION("MIME-Version"),
    IDENTITY("identity"),

    X_POWERED_BY("X-Powered-By"),
    HTTP2_SETTINGS("HTTP2-Settings"),

    STRICT_TRANSPORT_SECURITY("Strict-Transport-Security"),

    /**
     * HTTP2 Fields.
     */
    C_METHOD(":method"),
    C_SCHEME(":scheme"),
    C_AUTHORITY(":authority"),
    C_PATH(":path"),
    C_STATUS(":status"),

    UNKNOWN("::UNKNOWN::");

    public final static Trie<HttpHeader> CACHE = new ArrayTrie<>(630);

    static {
        for (HttpHeader header : HttpHeader.values())
            if (header != UNKNOWN)
                if (!CACHE.put(header.toString(), header))
                    throw new IllegalStateException();
    }

    private final String _string;
    private final byte[] _bytes;
    private final byte[] _bytesColonSpace;
    private final ByteBuffer _buffer;

    HttpHeader(String s) {
        _string = s;
        _bytes = StringUtils.getBytes(s);
        _bytesColonSpace = StringUtils.getBytes(s + ": ");
        _buffer = ByteBuffer.wrap(_bytes);
    }

    public ByteBuffer toBuffer() {
        return _buffer.asReadOnlyBuffer();
    }

    public byte[] getBytes() {
        return _bytes;
    }

    public byte[] getBytesColonSpace() {
        return _bytesColonSpace;
    }

    public boolean is(String s) {
        return _string.equalsIgnoreCase(s);
    }

    public String asString() {
        return _string;
    }

    @Override
    public String toString() {
        return _string;
    }
}
