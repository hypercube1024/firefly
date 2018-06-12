package com.firefly.net;

import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteBufferArrayOutputEntry extends AbstractByteBufferArrayOutputEntry {

    public ByteBufferArrayOutputEntry(Callback callback, ByteBuffer[] data) {
        super(callback, data);
    }

    @Override
    public OutputEntryType getOutputEntryType() {
        return OutputEntryType.BYTE_BUFFER_ARRAY;
    }

    @Override
    public long remaining() {
        return BufferUtils.remaining(data);
    }

}
