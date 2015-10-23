package com.firefly.codec.http2.encode;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PriorityFrame;
import com.firefly.codec.http2.hpack.HpackEncoder;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.io.BufferUtils;

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
	public List<ByteBuffer> generate(Frame frame) {
		HeadersFrame headersFrame = (HeadersFrame) frame;
		return generateHeaders(headersFrame.getStreamId(), headersFrame.getMetaData(), headersFrame.getPriority(),
				headersFrame.isEndStream());
	}

	public List<ByteBuffer> generateHeaders(int streamId, MetaData metaData, PriorityFrame priority,
			boolean endStream) {
		List<ByteBuffer> list = new LinkedList<>();
		if (streamId < 0)
			throw new IllegalArgumentException("Invalid stream id: " + streamId);

		int flags = Flags.NONE;

		if (priority != null)
			flags = Flags.PRIORITY;

		int maxFrameSize = getMaxFrameSize();
		ByteBuffer hpacked = ByteBuffer.allocate(maxFrameSize);
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
			list.add(header);

			hpacked.limit(maxHeaderBlockFragment);
			list.add(hpacked.slice());

			int position = maxHeaderBlockFragment;
			int limit = position + maxHeaderBlockFragment;
			while (limit < hpackedLength) {
				hpacked.position(position).limit(limit);
				header = generateHeader(FrameType.CONTINUATION, maxHeaderBlockFragment, Flags.NONE, streamId);
				BufferUtils.flipToFlush(header, 0);
				list.add(header);
				list.add(hpacked.slice());
				position += maxHeaderBlockFragment;
				limit += maxHeaderBlockFragment;
			}

			hpacked.position(position).limit(hpackedLength);
			header = generateHeader(FrameType.CONTINUATION, hpacked.remaining(), Flags.END_HEADERS, streamId);
			BufferUtils.flipToFlush(header, 0);
			list.add(header);
			list.add(hpacked);
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
			list.add(header);
			list.add(hpacked);
		}
		return list;
	}

	private void generatePriority(ByteBuffer header, PriorityFrame priority) {
		if (priority != null) {
			priorityGenerator.generatePriorityBody(header, priority.getStreamId(), priority.getParentStreamId(),
					priority.getWeight(), priority.isExclusive());
		}
	}
}
