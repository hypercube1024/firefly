package com.fireflysource.net.http.common.v2.hpack;

public abstract class HpackException extends RuntimeException {
    HpackException(String messageFormat, Object... args) {
        super(String.format(messageFormat, args));
    }

    /**
     * A Stream HPACK exception.
     * <p>Stream exceptions are not fatal to the connection, and the
     * hpack state is complete and able to continue handling other
     * decoding/encoding for the session.
     * </p>
     */
    public static class StreamException extends HpackException {
        StreamException(String messageFormat, Object... args) {
            super(messageFormat, args);
        }
    }

    /**
     * A Session HPACK Exception.
     * <p>Session exceptions are fatal for the stream, and the HPACK
     * state is unable to decode/encode further. </p>
     */
    public static class SessionException extends HpackException {
        SessionException(String messageFormat, Object... args) {
            super(messageFormat, args);
        }
    }

    public static class CompressionException extends SessionException {
        public CompressionException(String messageFormat, Object... args) {
            super(messageFormat, args);
        }
    }
}
