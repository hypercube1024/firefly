package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.model.MetaData;
import com.fireflysource.net.http.common.v2.frame.*;
import com.fireflysource.net.http.common.v2.hpack.HpackEncoder;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class HeadersGenerator extends FrameGenerator {

    private final HpackEncoder encoder;
    private final int maxHeaderBlockFragment;
    private final PriorityGenerator priorityGenerator;

    public HeadersGenerator(HeaderGenerator headerGenerator, HpackEncoder encoder) {
        this(headerGenerator, encoder, 0);
    }

    public HeadersGenerator(HeaderGenerator headerGenerator, HpackEncoder encoder, int maxHeaderBlockFragment) {
        super(headerGenerator);
        this.encoder = encoder;
        this.maxHeaderBlockFragment = maxHeaderBlockFragment;
        this.priorityGenerator = new PriorityGenerator(headerGenerator);
    }

    @Override
    public FrameBytes generate(Frame frame) {
        HeadersFrame headersFrame = (HeadersFrame) frame;
        return generateHeaders(headersFrame.getStreamId(), headersFrame.getMetaData(), headersFrame.getPriority(), headersFrame.isEndStream());
    }

    public FrameBytes generateHeaders(int streamId, MetaData metaData, PriorityFrame priority, boolean endStream) {
        if (streamId < 0)
            throw new IllegalArgumentException("Invalid stream id: " + streamId);

        int flags = Flags.NONE;
        FrameBytes frameBytes = new FrameBytes();
        frameBytes.setByteBuffers(new LinkedList<>());

        if (priority != null)
            flags = Flags.PRIORITY;

        int maxFrameSize = getMaxFrameSize();

        ByteBuffer hpacked = BufferUtils.allocate(maxFrameSize);
        BufferUtils.clearToFill(hpacked);
        encoder.encode(hpacked, metaData);
        int hpackedLength = hpacked.position();
        BufferUtils.flipToFlush(hpacked, 0);

        // Split into CONTINUATION frames if necessary.
        if (maxHeaderBlockFragment > 0 && hpackedLength > maxHeaderBlockFragment) {
            if (endStream)
                flags |= Flags.END_STREAM;

            int length = maxHeaderBlockFragment;
            if (priority != null)
                length += PriorityFrame.PRIORITY_LENGTH;

            ByteBuffer header = generateHeader(FrameType.HEADERS, length, flags, streamId);
            generatePriority(header, priority);
            BufferUtils.flipToFlush(header, 0);
            frameBytes.getByteBuffers().add(header);
            hpacked.limit(maxHeaderBlockFragment);
            frameBytes.getByteBuffers().add(hpacked.slice());

            int totalLength = Frame.HEADER_LENGTH + length;

            int position = maxHeaderBlockFragment;
            int limit = position + maxHeaderBlockFragment;
            while (limit < hpackedLength) {
                hpacked.position(position).limit(limit);
                header = generateHeader(FrameType.CONTINUATION, maxHeaderBlockFragment, Flags.NONE, streamId);
                BufferUtils.flipToFlush(header, 0);
                frameBytes.getByteBuffers().add(header);
                frameBytes.getByteBuffers().add(hpacked.slice());
                position += maxHeaderBlockFragment;
                limit += maxHeaderBlockFragment;
                totalLength += Frame.HEADER_LENGTH + maxHeaderBlockFragment;
            }

            hpacked.position(position).limit(hpackedLength);
            header = generateHeader(FrameType.CONTINUATION, hpacked.remaining(), Flags.END_HEADERS, streamId);
            BufferUtils.flipToFlush(header, 0);
            frameBytes.getByteBuffers().add(header);
            frameBytes.getByteBuffers().add(hpacked);
            totalLength += Frame.HEADER_LENGTH + hpacked.remaining();
            frameBytes.setLength(totalLength);
            return frameBytes;
        } else {
            flags |= Flags.END_HEADERS;
            if (endStream)
                flags |= Flags.END_STREAM;

            int length = hpackedLength;
            if (priority != null)
                length += PriorityFrame.PRIORITY_LENGTH;

            ByteBuffer header = generateHeader(FrameType.HEADERS, length, flags, streamId);
            generatePriority(header, priority);
            BufferUtils.flipToFlush(header, 0);
            frameBytes.getByteBuffers().add(header);
            frameBytes.getByteBuffers().add(hpacked);
            frameBytes.setLength(Frame.HEADER_LENGTH + length);
            return frameBytes;
        }
    }

    private void generatePriority(ByteBuffer header, PriorityFrame priority) {
        if (priority != null) {
            priorityGenerator.generatePriorityBody(header, priority.getStreamId(),
                    priority.getParentStreamId(), priority.getWeight(), priority.isExclusive());
        }
    }
}
