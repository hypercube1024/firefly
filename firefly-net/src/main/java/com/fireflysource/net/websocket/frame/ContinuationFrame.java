package com.fireflysource.net.websocket.frame;

import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.websocket.model.OpCode;

import java.nio.ByteBuffer;

public class ContinuationFrame extends DataFrame {
    public ContinuationFrame() {
        super(OpCode.CONTINUATION);
    }

    @Override
    public ContinuationFrame setPayload(ByteBuffer buf) {
        super.setPayload(buf);
        return this;
    }

    public ContinuationFrame setPayload(byte[] buf) {
        return this.setPayload(ByteBuffer.wrap(buf));
    }

    public ContinuationFrame setPayload(String message) {
        return this.setPayload(StringUtils.getUtf8Bytes(message));
    }

    @Override
    public Type getType() {
        return Type.CONTINUATION;
    }
}
