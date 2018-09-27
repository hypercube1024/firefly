package com.firefly.codec.http2.encode;

import java.nio.ByteBuffer;
import java.util.List;

import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;

public abstract class FrameGenerator {
    private final HeaderGenerator headerGenerator;

    public FrameGenerator(HeaderGenerator headerGenerator) {
        this.headerGenerator = headerGenerator;
    }

    public int getMaxFrameSize() {
        return headerGenerator.getMaxFrameSize();
    }

    protected ByteBuffer generateHeader(FrameType frameType, int length, int flags, int streamId) {
        return headerGenerator.generate(frameType, Frame.HEADER_LENGTH + length, length, flags, streamId);
    }

    public abstract List<ByteBuffer> generate(Frame frame);
}
