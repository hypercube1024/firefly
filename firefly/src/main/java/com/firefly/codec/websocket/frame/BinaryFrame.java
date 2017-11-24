package com.firefly.codec.websocket.frame;

import com.firefly.codec.websocket.model.OpCode;
import com.firefly.utils.StringUtils;

import java.nio.ByteBuffer;

public class BinaryFrame extends DataFrame {
    public BinaryFrame() {
        super(OpCode.BINARY);
    }

    public BinaryFrame setPayload(ByteBuffer buf) {
        super.setPayload(buf);
        return this;
    }

    public BinaryFrame setPayload(byte[] buf) {
        setPayload(ByteBuffer.wrap(buf));
        return this;
    }

    public BinaryFrame setPayload(String payload) {
        setPayload(StringUtils.getUtf8Bytes(payload));
        return this;
    }

    @Override
    public Type getType() {
        return getOpCode() == OpCode.CONTINUATION ? Type.CONTINUATION : Type.BINARY;
    }
}
