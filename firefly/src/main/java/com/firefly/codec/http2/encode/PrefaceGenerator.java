package com.firefly.codec.http2.encode;

import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.PrefaceFrame;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class PrefaceGenerator extends FrameGenerator {
    public PrefaceGenerator() {
        super(null);
    }

    @Override
    public List<ByteBuffer> generate(Frame frame) {
        return Collections.singletonList(ByteBuffer.wrap(PrefaceFrame.PREFACE_BYTES));
    }
}
