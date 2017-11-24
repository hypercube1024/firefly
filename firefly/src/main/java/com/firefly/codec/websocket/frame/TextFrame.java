package com.firefly.codec.websocket.frame;

import com.firefly.codec.websocket.model.OpCode;
import com.firefly.utils.StringUtils;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;

public class TextFrame extends DataFrame {
    public TextFrame() {
        super(OpCode.TEXT);
    }

    @Override
    public Type getType() {
        return getOpCode() == OpCode.CONTINUATION ? Type.CONTINUATION : Type.TEXT;
    }

    public TextFrame setPayload(String str) {
        setPayload(ByteBuffer.wrap(StringUtils.getUtf8Bytes(str)));
        return this;
    }

    public String getPayloadAsUTF8() {
        if (data == null) {
            return null;
        }
        return BufferUtils.toUTF8String(data);
    }
}
