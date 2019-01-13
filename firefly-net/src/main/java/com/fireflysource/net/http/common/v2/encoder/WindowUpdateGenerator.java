package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.v2.frame.Flags;
import com.fireflysource.net.http.common.v2.frame.Frame;
import com.fireflysource.net.http.common.v2.frame.FrameType;
import com.fireflysource.net.http.common.v2.frame.WindowUpdateFrame;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class WindowUpdateGenerator extends FrameGenerator {
    public WindowUpdateGenerator(HeaderGenerator headerGenerator) {
        super(headerGenerator);
    }

    @Override
    public FrameBytes generate(Frame frame) {
        WindowUpdateFrame windowUpdateFrame = (WindowUpdateFrame) frame;
        return generateWindowUpdate(windowUpdateFrame.getStreamId(), windowUpdateFrame.getWindowDelta());
    }

    public FrameBytes generateWindowUpdate(int streamId, int windowUpdate) {
        if (windowUpdate < 0)
            throw new IllegalArgumentException("Invalid window update: " + windowUpdate);

        ByteBuffer header = generateHeader(FrameType.WINDOW_UPDATE, WindowUpdateFrame.WINDOW_UPDATE_LENGTH, Flags.NONE, streamId);
        header.putInt(windowUpdate);
        BufferUtils.flipToFlush(header, 0);

        FrameBytes frameBytes = new FrameBytes();
        frameBytes.setByteBuffers(new LinkedList<>());
        frameBytes.getByteBuffers().add(header);
        frameBytes.setLength(Frame.HEADER_LENGTH + WindowUpdateFrame.WINDOW_UPDATE_LENGTH);
        return frameBytes;
    }
}
