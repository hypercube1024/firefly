package com.firefly.utils.io;

import com.firefly.utils.concurrent.CountingCallback;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface BufferReaderHandler {
    public void readBuffer(ByteBuffer buf, CountingCallback countingCallback, long count) throws IOException;
}
