package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.v2.frame.Flags;
import com.fireflysource.net.http.common.v2.frame.Frame;
import com.fireflysource.net.http.common.v2.frame.FrameType;
import com.fireflysource.net.http.common.v2.frame.ResetFrame;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class ResetGenerator extends FrameGenerator {
    public ResetGenerator(HeaderGenerator headerGenerator) {
        super(headerGenerator);
    }

    @Override
    public FrameBytes generate(Frame frame) {
        ResetFrame resetFrame = (ResetFrame) frame;
        return generateReset(resetFrame.getStreamId(), resetFrame.getError());
    }

    public FrameBytes generateReset(int streamId, int error) {
        if (streamId < 0)
            throw new IllegalArgumentException("Invalid stream id: " + streamId);

        ByteBuffer header = generateHeader(FrameType.RST_STREAM, ResetFrame.RESET_LENGTH, Flags.NONE, streamId);
        header.putInt(error);
        BufferUtils.flipToFlush(header, 0);
        FrameBytes frameBytes = new FrameBytes();
        frameBytes.setByteBuffers(new LinkedList<>());
        frameBytes.getByteBuffers().add(header);
        frameBytes.setLength(Frame.HEADER_LENGTH + ResetFrame.RESET_LENGTH);
        return frameBytes;
    }
}
