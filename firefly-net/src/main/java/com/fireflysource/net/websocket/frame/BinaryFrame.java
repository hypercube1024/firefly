package com.fireflysource.net.websocket.frame;

import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.websocket.model.OpCode;

import java.nio.ByteBuffer;

public class BinaryFrame extends DataFrame {
    public BinaryFrame() {
        super(OpCode.BINARY);
    }

    @Override
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
        if (getOpCode() == OpCode.CONTINUATION)
            return Type.CONTINUATION;
        else
            return Type.BINARY;
    }
}
