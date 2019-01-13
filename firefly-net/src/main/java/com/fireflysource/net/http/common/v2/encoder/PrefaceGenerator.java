package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.net.http.common.v2.frame.Frame;
import com.fireflysource.net.http.common.v2.frame.PrefaceFrame;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class PrefaceGenerator extends FrameGenerator {
    public PrefaceGenerator() {
        super(null);
    }

    @Override
    public FrameBytes generate(Frame frame) {
        FrameBytes frameBytes = new FrameBytes();
        frameBytes.setByteBuffers(new LinkedList<>());
        frameBytes.getByteBuffers().add(ByteBuffer.wrap(PrefaceFrame.PREFACE_BYTES));
        frameBytes.setLength(PrefaceFrame.PREFACE_BYTES.length);
        return frameBytes;
    }
}
