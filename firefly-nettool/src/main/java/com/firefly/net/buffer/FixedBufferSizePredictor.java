package com.firefly.net.buffer;

import com.firefly.net.BufferSizePredictor;


public class FixedBufferSizePredictor implements
        BufferSizePredictor {

    private final int bufferSize;

    public FixedBufferSizePredictor(int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException(
                    "bufferSize must greater than 0: " + bufferSize);
        }
        this.bufferSize = bufferSize;
    }

    @Override
    public int nextBufferSize() {
        return bufferSize;
    }

    @Override
    public void previousReceivedBufferSize(int previousReceivedBufferSize) {
        // Ignore
    }

}
