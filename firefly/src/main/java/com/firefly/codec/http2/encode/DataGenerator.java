package com.firefly.codec.http2.encode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.utils.io.BufferUtils;

public class DataGenerator {
	private final HeaderGenerator headerGenerator;

	public DataGenerator(HeaderGenerator headerGenerator) {
		this.headerGenerator = headerGenerator;
	}

	public List<ByteBuffer> generateData(int streamId, ByteBuffer data, boolean last, int maxLength) {
		if (streamId < 0)
			throw new IllegalArgumentException("Invalid stream id: " + streamId);

		List<ByteBuffer> list = new ArrayList<>();
		int dataLength = data.remaining();
		int maxFrameSize = headerGenerator.getMaxFrameSize();

		if (dataLength <= maxLength && dataLength <= maxFrameSize) {
			// Single frame.
			generateFrame(streamId, data, last, list);
			return list;
		}

		// Other cases, we need to slice the original buffer into multiple frames.
		int length = Math.min(maxLength, dataLength);
		int frames = length / maxFrameSize;
		if (frames * maxFrameSize != length)
			++frames;

		int begin = data.position();
		int end = data.limit();
		for (int i = 1; i <= frames; i++) {
			int limit = begin + Math.min(maxFrameSize * i, length);
			data.limit(limit);
			ByteBuffer slice = data.slice();
			data.position(limit);
			generateFrame(streamId, slice, i == frames && last && limit == end, list);
		}
		data.limit(end);
		return list;
	}

	private void generateFrame(int streamId, ByteBuffer data, boolean last, List<ByteBuffer> list) {
		int length = data.remaining();

		int flags = Flags.NONE;
		if (last)
			flags |= Flags.END_STREAM;

		ByteBuffer header = headerGenerator.generate(FrameType.DATA, Frame.HEADER_LENGTH + length, length, flags, streamId);
		BufferUtils.flipToFlush(header, 0);
		list.add(header);
		list.add(data);
	}

}
