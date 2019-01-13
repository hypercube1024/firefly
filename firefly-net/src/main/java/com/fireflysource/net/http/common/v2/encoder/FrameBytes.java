package com.fireflysource.net.http.common.v2.encoder;

import java.nio.ByteBuffer;
import java.util.List;

public class FrameBytes {

    public static final FrameBytes EMPTY = new FrameBytes();

    private int length;
    private List<ByteBuffer> byteBuffers;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public List<ByteBuffer> getByteBuffers() {
        return byteBuffers;
    }

    public void setByteBuffers(List<ByteBuffer> byteBuffers) {
        this.byteBuffers = byteBuffers;
    }
}
