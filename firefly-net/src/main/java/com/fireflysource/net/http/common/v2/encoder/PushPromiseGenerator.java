package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.model.MetaData;
import com.fireflysource.net.http.common.v2.frame.Flags;
import com.fireflysource.net.http.common.v2.frame.Frame;
import com.fireflysource.net.http.common.v2.frame.FrameType;
import com.fireflysource.net.http.common.v2.frame.PushPromiseFrame;
import com.fireflysource.net.http.common.v2.hpack.HpackEncoder;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class PushPromiseGenerator extends FrameGenerator {

    private final HpackEncoder encoder;

    public PushPromiseGenerator(HeaderGenerator headerGenerator, HpackEncoder encoder) {
        super(headerGenerator);
        this.encoder = encoder;
    }

    @Override
    public FrameBytes generate(Frame frame) {
        PushPromiseFrame pushPromiseFrame = (PushPromiseFrame) frame;
        return generatePushPromise(pushPromiseFrame.getStreamId(), pushPromiseFrame.getPromisedStreamId(), pushPromiseFrame.getMetaData());
    }

    public FrameBytes generatePushPromise(int streamId, int promisedStreamId, MetaData metaData) {
        if (streamId < 0)
            throw new IllegalArgumentException("Invalid stream id: " + streamId);
        if (promisedStreamId < 0)
            throw new IllegalArgumentException("Invalid promised stream id: " + promisedStreamId);

        int maxFrameSize = getMaxFrameSize();
        // The promised streamId space.
        int extraSpace = 4;
        maxFrameSize -= extraSpace;

        ByteBuffer hpacked = BufferUtils.allocate(maxFrameSize);
        BufferUtils.clearToFill(hpacked);
        encoder.encode(hpacked, metaData);
        int hpackedLength = hpacked.position();
        BufferUtils.flipToFlush(hpacked, 0);

        int length = hpackedLength + extraSpace;
        int flags = Flags.END_HEADERS;

        ByteBuffer header = generateHeader(FrameType.PUSH_PROMISE, length, flags, streamId);
        header.putInt(promisedStreamId);
        BufferUtils.flipToFlush(header, 0);

        FrameBytes frameBytes = new FrameBytes();
        frameBytes.setByteBuffers(new LinkedList<>());
        frameBytes.getByteBuffers().add(header);
        frameBytes.getByteBuffers().add(hpacked);
        frameBytes.setLength(Frame.HEADER_LENGTH + length);
        return frameBytes;
    }
}
