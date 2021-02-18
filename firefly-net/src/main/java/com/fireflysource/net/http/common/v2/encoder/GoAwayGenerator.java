package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.v2.frame.Flags;
import com.fireflysource.net.http.common.v2.frame.Frame;
import com.fireflysource.net.http.common.v2.frame.FrameType;
import com.fireflysource.net.http.common.v2.frame.GoAwayFrame;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;

public class GoAwayGenerator extends FrameGenerator {

    public GoAwayGenerator(HeaderGenerator headerGenerator) {
        super(headerGenerator);
    }

    @Override
    public FrameBytes generate(Frame frame) {
        GoAwayFrame goAwayFrame = (GoAwayFrame) frame;
        return generateGoAway(goAwayFrame.getLastStreamId(), goAwayFrame.getError(), goAwayFrame.getPayload());
    }

    public FrameBytes generateGoAway(int lastStreamId, int error, byte[] payload) {
        if (lastStreamId < 0) {
            lastStreamId = 0;
        }

        // The last streamId + the error code.
        int fixedLength = 4 + 4;

        // Make sure we don't exceed the default frame max length.
        int maxPayloadLength = Frame.DEFAULT_MAX_LENGTH - fixedLength;
        if (payload != null && payload.length > maxPayloadLength)
            payload = Arrays.copyOfRange(payload, 0, maxPayloadLength);

        int length = fixedLength + (payload != null ? payload.length : 0);
        ByteBuffer header = generateHeader(FrameType.GO_AWAY, length, Flags.NONE, 0);

        header.putInt(lastStreamId);
        header.putInt(error);

        if (payload != null) {
            header.put(payload);
        }

        BufferUtils.flipToFlush(header, 0);
        FrameBytes frameBytes = new FrameBytes();
        frameBytes.setLength(Frame.HEADER_LENGTH + length);
        frameBytes.setByteBuffers(new LinkedList<>());
        frameBytes.getByteBuffers().add(header);
        return frameBytes;
    }
}
