package com.firefly.codec.http2.frame;

import java.util.Objects;

public class PingFrame extends Frame {
    public static final int PING_LENGTH = 8;
    private static final byte[] EMPTY_PAYLOAD = new byte[8];

    private final byte[] payload;
    private final boolean reply;

    /**
     * Creates a PING frame with an empty payload.
     *
     * @param reply whether this PING frame is a reply
     */
    public PingFrame(boolean reply) {
        this(EMPTY_PAYLOAD, reply);
    }

    /**
     * Creates a PING frame with the given {@code long} {@code value} as
     * payload.
     *
     * @param value the value to use as a payload for this PING frame
     * @param reply whether this PING frame is a reply
     */
    public PingFrame(long value, boolean reply) {
        this(toBytes(value), reply);
    }

    /**
     * Creates a PING frame with the given {@code payload}.
     *
     * @param payload the payload for this PING frame
     * @param reply   whether this PING frame is a reply
     */
    public PingFrame(byte[] payload, boolean reply) {
        super(FrameType.PING);
        this.payload = Objects.requireNonNull(payload);
        if (payload.length != PING_LENGTH)
            throw new IllegalArgumentException("PING payload must be 8 bytes");
        this.reply = reply;
    }

    public byte[] getPayload() {
        return payload;
    }

    public long getPayloadAsLong() {
        return toLong(payload);
    }

    public boolean isReply() {
        return reply;
    }

    private static byte[] toBytes(long value) {
        byte[] result = new byte[8];
        for (int i = result.length - 1; i >= 0; --i) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private static long toLong(byte[] payload) {
        long result = 0;
        for (int i = 0; i < 8; ++i) {
            result <<= 8;
            result |= (payload[i] & 0xFF);
        }
        return result;
    }
}
