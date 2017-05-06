package com.firefly.codec.http2.encode;

import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class ResetGenerator extends FrameGenerator {
    public ResetGenerator(HeaderGenerator headerGenerator) {
        super(headerGenerator);
    }

    @Override
    public List<ByteBuffer> generate(Frame frame) {
        ResetFrame resetFrame = (ResetFrame) frame;
        return Collections.singletonList(generateReset(resetFrame.getStreamId(), resetFrame.getError()));
    }

    public ByteBuffer generateReset(int streamId, int error) {
        if (streamId < 0)
            throw new IllegalArgumentException("Invalid stream id: " + streamId);

        ByteBuffer header = generateHeader(FrameType.RST_STREAM, 4, Flags.NONE, streamId);
        header.putInt(error);
        BufferUtils.flipToFlush(header, 0);
        return header;
    }
}
