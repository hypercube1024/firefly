package com.firefly.net;

public interface BufferSizePredictor {

    int nextBufferSize();

    void previousReceivedBufferSize(int previousReceivedBufferSize);
}
