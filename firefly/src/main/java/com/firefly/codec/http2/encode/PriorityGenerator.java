package com.firefly.codec.http2.encode;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.frame.PriorityFrame;
import com.firefly.utils.io.BufferUtils;

public class PriorityGenerator extends FrameGenerator {
	public PriorityGenerator(HeaderGenerator headerGenerator) {
		super(headerGenerator);
	}

	@Override
	public ByteBuffer generate(Frame frame) {
		PriorityFrame priorityFrame = (PriorityFrame) frame;
		return generatePriority(priorityFrame.getStreamId(), priorityFrame.getParentStreamId(),
				priorityFrame.getWeight(), priorityFrame.isExclusive());
	}

	public ByteBuffer generatePriority(int streamId, int parentStreamId, int weight, boolean exclusive) {
		ByteBuffer header = generateHeader(FrameType.PRIORITY, PriorityFrame.PRIORITY_LENGTH, Flags.NONE, streamId);
		generatePriorityBody(header, streamId, parentStreamId, weight, exclusive);
		BufferUtils.flipToFlush(header, 0);
		return header;
	}

	public void generatePriorityBody(ByteBuffer header, int streamId, int parentStreamId, int weight,
			boolean exclusive) {
		if (streamId < 0)
			throw new IllegalArgumentException("Invalid stream id: " + streamId);
		if (parentStreamId < 0)
			throw new IllegalArgumentException("Invalid parent stream id: " + parentStreamId);
		if (parentStreamId == streamId)
			throw new IllegalArgumentException("Stream " + streamId + " cannot depend on stream " + parentStreamId);
		if (weight < 1 || weight > 256)
			throw new IllegalArgumentException("Invalid weight: " + weight);

		if (exclusive)
			parentStreamId |= 0x80_00_00_00;
		header.putInt(parentStreamId);
		header.put((byte) (weight - 1));
	}
}
