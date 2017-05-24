package com.firefly.codec.http2.encode;

import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class WindowUpdateGenerator extends FrameGenerator {
    public WindowUpdateGenerator(HeaderGenerator headerGenerator) {
        super(headerGenerator);
    }

    @Override
    public List<ByteBuffer> generate(Frame frame) {
        WindowUpdateFrame windowUpdateFrame = (WindowUpdateFrame) frame;
        return Collections.singletonList(generateWindowUpdate(windowUpdateFrame.getStreamId(), windowUpdateFrame.getWindowDelta()));
    }

    public ByteBuffer generateWindowUpdate(int streamId, int windowUpdate) {
        if (windowUpdate < 0)
            throw new IllegalArgumentException("Invalid window update: " + windowUpdate);

        ByteBuffer header = generateHeader(FrameType.WINDOW_UPDATE, 4, Flags.NONE, streamId);
        header.putInt(windowUpdate);
        BufferUtils.flipToFlush(header, 0);
        return header;
    }
}
