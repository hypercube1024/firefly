package com.firefly.codec.http2.encode;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.frame.PingFrame;
import com.firefly.utils.io.BufferUtils;

public class PingGenerator extends FrameGenerator {
	public PingGenerator(HeaderGenerator headerGenerator) {
		super(headerGenerator);
	}

	@Override
	public List<ByteBuffer> generate(Frame frame) {
		PingFrame pingFrame = (PingFrame) frame;
		return Collections.singletonList(generatePing(pingFrame.getPayload(), pingFrame.isReply()));
	}

	public ByteBuffer generatePing(byte[] payload, boolean reply) {
		if (payload.length != PingFrame.PING_LENGTH)
			throw new IllegalArgumentException("Invalid payload length: " + payload.length);

		ByteBuffer header = generateHeader(FrameType.PING, PingFrame.PING_LENGTH, reply ? Flags.ACK : Flags.NONE, 0);

		header.put(payload);

		BufferUtils.flipToFlush(header, 0);
		return header;
	}
}
