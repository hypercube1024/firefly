package com.firefly.codec.http2.encode;

import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;

import java.nio.ByteBuffer;

public class HeaderGenerator {
    private int maxFrameSize = Frame.DEFAULT_MAX_LENGTH;

    public ByteBuffer generate(FrameType frameType, int capacity, int length, int flags, int streamId) {
        ByteBuffer header = ByteBuffer.allocate(capacity);
        header.put((byte) ((length & 0x00_FF_00_00) >>> 16));
        header.put((byte) ((length & 0x00_00_FF_00) >>> 8));
        header.put((byte) ((length & 0x00_00_00_FF)));
        header.put((byte) frameType.getType());
        header.put((byte) flags);
        header.putInt(streamId);
        return header;
    }

    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setMaxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

}
