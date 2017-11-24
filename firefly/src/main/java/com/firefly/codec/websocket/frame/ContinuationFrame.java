package com.firefly.codec.websocket.frame;

import com.firefly.codec.websocket.model.OpCode;
import com.firefly.utils.StringUtils;

import java.nio.ByteBuffer;

public class ContinuationFrame extends DataFrame {
    public ContinuationFrame() {
        super(OpCode.CONTINUATION);
    }

    public ContinuationFrame setPayload(ByteBuffer buf) {
        super.setPayload(buf);
        return this;
    }

    public ContinuationFrame setPayload(byte buf[]) {
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
