package com.firefly.codec.http2.encode;

import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.frame.GoAwayFrame;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GoAwayGenerator extends FrameGenerator {
    public GoAwayGenerator(HeaderGenerator headerGenerator) {
        super(headerGenerator);
    }

    @Override
    public List<ByteBuffer> generate(Frame frame) {
        GoAwayFrame goAwayFrame = (GoAwayFrame) frame;
        return Collections.singletonList(generateGoAway(goAwayFrame.getLastStreamId(), goAwayFrame.getError(), goAwayFrame.getPayload()));
    }

    public ByteBuffer generateGoAway(int lastStreamId, int error, byte[] payload) {
        if (lastStreamId < 0)
            throw new IllegalArgumentException("Invalid last stream id: " + lastStreamId);

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
        return header;
    }
}
