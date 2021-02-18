package com.fireflysource.net.websocket.common.frame;

import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.websocket.common.model.OpCode;

import java.nio.ByteBuffer;

public class PingFrame extends ControlFrame {
    public PingFrame() {
        super(OpCode.PING);
    }

    public PingFrame setPayload(byte[] bytes) {
        setPayload(ByteBuffer.wrap(bytes));
        return this;
    }

    public PingFrame setPayload(String payload) {
        setPayload(ByteBuffer.wrap(StringUtils.getUtf8Bytes(payload)));
        return this;
    }

    @Override
    public Type getType() {
        return Type.PING;
    }
}
