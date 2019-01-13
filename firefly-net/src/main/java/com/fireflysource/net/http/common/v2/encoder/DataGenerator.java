package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.v2.frame.DataFrame;
import com.fireflysource.net.http.common.v2.frame.Flags;
import com.fireflysource.net.http.common.v2.frame.Frame;
import com.fireflysource.net.http.common.v2.frame.FrameType;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class DataGenerator {
    private final HeaderGenerator headerGenerator;

    public DataGenerator(HeaderGenerator headerGenerator) {
        this.headerGenerator = headerGenerator;
    }

    public FrameBytes generate(DataFrame frame, int maxLength) {
        return generateData(frame.getStreamId(), frame.getData(), frame.isEndStream(), maxLength);
    }

    public FrameBytes generateData(int streamId, ByteBuffer data, boolean last, int maxLength) {
        if (streamId < 0)
            throw new IllegalArgumentException("Invalid stream id: " + streamId);

        FrameBytes frameBytes = new FrameBytes();
        frameBytes.setByteBuffers(new LinkedList<>());

        int dataLength = data.remaining();
        int maxFrameSize = headerGenerator.getMaxFrameSize();
        int length = Math.min(dataLength, Math.min(maxFrameSize, maxLength));
        if (length == dataLength) {
            generateFrame(streamId, data, last, frameBytes);
        } else {
            int limit = data.limit();
            int newLimit = data.position() + length;
            data.limit(newLimit);
            ByteBuffer slice = data.slice();
            data.position(newLimit);
            data.limit(limit);
            generateFrame(streamId, slice, false, frameBytes);
        }
        frameBytes.setLength(Frame.HEADER_LENGTH + length);
        return frameBytes;
    }

    private void generateFrame(int streamId, ByteBuffer data, boolean last, FrameBytes frameBytes) {
        int length = data.remaining();

        int flags = Flags.NONE;
        if (last)
            flags |= Flags.END_STREAM;

        ByteBuffer header = headerGenerator.generate(FrameType.DATA, Frame.HEADER_LENGTH + length, length, flags, streamId);
        BufferUtils.flipToFlush(header, 0);
        frameBytes.getByteBuffers().add(header);
        // Skip empty data buffers.
        if (data.remaining() > 0)
            frameBytes.getByteBuffers().add(data);
    }
}
