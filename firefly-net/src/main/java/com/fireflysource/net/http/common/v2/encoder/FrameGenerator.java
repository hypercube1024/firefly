package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.net.http.common.v2.frame.Frame;
import com.fireflysource.net.http.common.v2.frame.FrameType;

import java.nio.ByteBuffer;

public abstract class FrameGenerator {
    private final HeaderGenerator headerGenerator;

    protected FrameGenerator(HeaderGenerator headerGenerator) {
        this.headerGenerator = headerGenerator;
    }

    public abstract FrameBytes generate(Frame frame);

    protected ByteBuffer generateHeader(FrameType frameType, int length, int flags, int streamId) {
        return headerGenerator.generate(frameType, Frame.HEADER_LENGTH + length, length, flags, streamId);
    }

    public int getMaxFrameSize() {
        return headerGenerator.getMaxFrameSize();
    }
}
