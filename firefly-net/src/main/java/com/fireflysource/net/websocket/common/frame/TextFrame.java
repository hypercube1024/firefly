package com.fireflysource.net.websocket.common.frame;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.websocket.common.model.OpCode;

import java.nio.ByteBuffer;

public class TextFrame extends DataFrame {
    public TextFrame() {
        super(OpCode.TEXT);
    }

    public TextFrame(Frame basedOn) {
        super(basedOn);
    }

    public TextFrame(Frame basedOn, boolean continuation) {
        super(basedOn, continuation);
    }

    @Override
    public Type getType() {
        if (getOpCode() == OpCode.CONTINUATION)
            return Type.CONTINUATION;
        return Type.TEXT;
    }

    public TextFrame setPayload(String str) {
        setPayload(ByteBuffer.wrap(StringUtils.getUtf8Bytes(str)));
        return this;
    }

    @Override
    public String getPayloadAsUTF8() {
        if (data == null) {
            return null;
        }
        return BufferUtils.toUTF8String(data);
    }
}
