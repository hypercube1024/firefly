package com.fireflysource.net.websocket.common.frame;

import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.websocket.common.model.OpCode;

import java.nio.ByteBuffer;

public class BinaryFrame extends DataFrame {

    public BinaryFrame() {
        super(OpCode.BINARY);
    }

    public BinaryFrame(Frame basedOn) {
        super(basedOn);
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
        return Type.BINARY;
    }
}
