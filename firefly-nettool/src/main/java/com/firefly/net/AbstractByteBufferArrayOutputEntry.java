package com.firefly.net;

import com.firefly.utils.concurrent.Callback;

import java.nio.ByteBuffer;

abstract public class AbstractByteBufferArrayOutputEntry extends AbstractOutputEntry<ByteBuffer[]> {

    protected int lastOffset = 0;

    public AbstractByteBufferArrayOutputEntry(Callback callback, ByteBuffer[] data) {
        super(callback, data);
    }

    public int getOffset() {
        for (int i = lastOffset; i < data.length; i++) {
            if (data[i].hasRemaining()) {
                lastOffset = i;
                return i;
            }
        }
        return 0;
    }
}
