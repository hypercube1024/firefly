package com.firefly.codec.http2.encode;

import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.frame.PushPromiseFrame;
import com.firefly.codec.http2.hpack.HpackEncoder;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class PushPromiseGenerator extends FrameGenerator {
    private final HpackEncoder encoder;

    public PushPromiseGenerator(HeaderGenerator headerGenerator, HpackEncoder encoder) {
        super(headerGenerator);
        this.encoder = encoder;
    }

    @Override
    public List<ByteBuffer> generate(Frame frame) {
        PushPromiseFrame pushPromiseFrame = (PushPromiseFrame) frame;
        return generatePushPromise(pushPromiseFrame.getStreamId(), pushPromiseFrame.getPromisedStreamId(),
                pushPromiseFrame.getMetaData());
    }

    public List<ByteBuffer> generatePushPromise(int streamId, int promisedStreamId, MetaData metaData) {
        if (streamId < 0)
            throw new IllegalArgumentException("Invalid stream id: " + streamId);
        if (promisedStreamId < 0)
            throw new IllegalArgumentException("Invalid promised stream id: " + promisedStreamId);

        List<ByteBuffer> list = new LinkedList<>();
        int maxFrameSize = getMaxFrameSize();
        // The promised streamId space.
        int extraSpace = 4;
        maxFrameSize -= extraSpace;

        ByteBuffer hpacked = ByteBuffer.allocate(maxFrameSize);
        BufferUtils.clearToFill(hpacked);
        encoder.encode(hpacked, metaData);
        int hpackedLength = hpacked.position();
        BufferUtils.flipToFlush(hpacked, 0);

        int length = hpackedLength + extraSpace;
        int flags = Flags.END_HEADERS;

        ByteBuffer header = generateHeader(FrameType.PUSH_PROMISE, length, flags, streamId);
        header.putInt(promisedStreamId);
        BufferUtils.flipToFlush(header, 0);

        list.add(header);
        list.add(hpacked);
        return list;
    }
}
