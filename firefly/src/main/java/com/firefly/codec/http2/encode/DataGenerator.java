package com.firefly.codec.http2.encode;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.lang.Pair;

public class DataGenerator {
    private final HeaderGenerator headerGenerator;

    public DataGenerator(HeaderGenerator headerGenerator) {
        this.headerGenerator = headerGenerator;
    }

    public Pair<Integer, List<ByteBuffer>> generate(DataFrame frame, int maxLength) {
        return generateData(frame.getStreamId(), frame.getData(), frame.isEndStream(), maxLength);
    }

    public Pair<Integer, List<ByteBuffer>> generateData(int streamId, ByteBuffer data, boolean last, int maxLength) {
        if (streamId < 0)
            throw new IllegalArgumentException("Invalid stream id: " + streamId);

        List<ByteBuffer> list = new LinkedList<>();

        int dataLength = data.remaining();
        int maxFrameSize = headerGenerator.getMaxFrameSize();
        int length = Math.min(dataLength, Math.min(maxFrameSize, maxLength));
        if (length == dataLength) {
            generateFrame(streamId, data, last, list);
        } else {
            int limit = data.limit();
            int newLimit = data.position() + length;
            data.limit(newLimit);
            ByteBuffer slice = data.slice();
            data.position(newLimit);
            data.limit(limit);
            generateFrame(streamId, slice, false, list);
        }
        return new Pair<>(length + Frame.HEADER_LENGTH, list);
    }

    private void generateFrame(int streamId, ByteBuffer data, boolean last, List<ByteBuffer> list) {
        int length = data.remaining();

        int flags = Flags.NONE;
        if (last)
            flags |= Flags.END_STREAM;

        ByteBuffer header = headerGenerator.generate(FrameType.DATA, Frame.HEADER_LENGTH + length, length, flags,
                streamId);

        BufferUtils.flipToFlush(header, 0);
        list.add(header);

        if (data.remaining() > 0)
            list.add(data);
    }

}
