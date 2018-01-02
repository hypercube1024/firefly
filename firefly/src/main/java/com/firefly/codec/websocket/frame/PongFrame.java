package com.firefly.codec.websocket.frame;

import com.firefly.codec.websocket.model.OpCode;
import com.firefly.utils.StringUtils;

import java.nio.ByteBuffer;

public class PongFrame extends ControlFrame {
    public PongFrame() {
        super(OpCode.PONG);
    }

    public PongFrame setPayload(byte[] bytes) {
        setPayload(ByteBuffer.wrap(bytes));
        return this;
    }

    public PongFrame setPayload(String payload) {
        setPayload(StringUtils.getUtf8Bytes(payload));
        return this;
    }

    @Override
    public Type getType() {
        return Type.PONG;
    }
}
