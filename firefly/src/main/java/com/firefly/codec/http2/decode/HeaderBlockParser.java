package com.firefly.codec.http2.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.hpack.HpackDecoder;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.io.BufferUtils;

public class HeaderBlockParser {
	private final HpackDecoder hpackDecoder;
	private ByteBuffer blockBuffer;

	public HeaderBlockParser(HpackDecoder hpackDecoder) {
		this.hpackDecoder = hpackDecoder;
	}

	public MetaData parse(ByteBuffer buffer, int blockLength) {
		// We must wait for the all the bytes of the header block to arrive.
		// If they are not all available, accumulate them.
		// When all are available, decode them.

		int accumulated = blockBuffer == null ? 0 : blockBuffer.position();
		int remaining = blockLength - accumulated;

		if (buffer.remaining() < remaining) {
			if (blockBuffer == null) {
				blockBuffer = ByteBuffer.allocate(blockLength);
				BufferUtils.clearToFill(blockBuffer);
			}
			blockBuffer.put(buffer);
			return null;
		} else {
			int limit = buffer.limit();
			buffer.limit(buffer.position() + remaining);
			ByteBuffer toDecode;
			if (blockBuffer != null) {
				blockBuffer.put(buffer);
				BufferUtils.flipToFlush(blockBuffer, 0);
				toDecode = blockBuffer;
			} else {
				toDecode = buffer;
			}

			MetaData result = hpackDecoder.decode(toDecode);
			buffer.limit(limit);
			return result;
		}
	}
}
