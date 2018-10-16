package com.firefly.net;

import java.nio.ByteBuffer;

public interface Decoder {
    void decode(ByteBuffer buf, Session session) throws Throwable;
}
