package com.firefly.codec.http2.frame;

import com.firefly.codec.http2.stream.CloseState;

import java.nio.charset.StandardCharsets;

public class GoAwayFrame extends Frame {

    private final CloseState closeState;
    private final int lastStreamId;
    private final int error;
    private final byte[] payload;

    public GoAwayFrame(int lastStreamId, int error, byte[] payload) {
        this(CloseState.REMOTELY_CLOSED, lastStreamId, error, payload);
    }

    public GoAwayFrame(CloseState closeState, int lastStreamId, int error, byte[] payload) {
        super(FrameType.GO_AWAY);
        this.closeState = closeState;
        this.lastStreamId = lastStreamId;
        this.error = error;
        this.payload = payload;
    }

    public int getLastStreamId() {
        return lastStreamId;
    }

    public int getError() {
        return error;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String tryConvertPayload() {
        if (payload == null || payload.length == 0)
            return "";
        try {
            return new String(payload, StandardCharsets.UTF_8);
        } catch (Throwable x) {
            return "";
        }
    }

    @Override
    public String toString() {
        return String.format("%s,%d/%s/%s/%s",
                super.toString(),
                lastStreamId,
                ErrorCode.toString(error, null),
                tryConvertPayload(),
                closeState);
    }
}
