package com.firefly.net;

import com.firefly.utils.concurrent.Callback;

import java.nio.ByteBuffer;

public class ByteBufferOutputEntry extends AbstractOutputEntry<ByteBuffer> {

    public ByteBufferOutputEntry(Callback callback, ByteBuffer data) {
        super(callback, data);
    }

    @Override
    public OutputEntryType getOutputEntryType() {
        return OutputEntryType.BYTE_BUFFER;
    }

    @Override
    public long remaining() {
        return data.remaining();
    }

}
