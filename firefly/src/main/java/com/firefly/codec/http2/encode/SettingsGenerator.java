package com.firefly.codec.http2.encode;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.utils.io.BufferUtils;

public class SettingsGenerator extends FrameGenerator {
	public SettingsGenerator(HeaderGenerator headerGenerator) {
		super(headerGenerator);
	}

	@Override
	public List<ByteBuffer> generate(Frame frame) {
		SettingsFrame settingsFrame = (SettingsFrame) frame;
		return Arrays.asList(generateSettings(settingsFrame.getSettings(), settingsFrame.isReply()));
	}

	public ByteBuffer generateSettings(Map<Integer, Integer> settings, boolean reply) {
		// Two bytes for the identifier, four bytes for the value.
		int entryLength = 2 + 4;
		int length = entryLength * settings.size();
		if (length > getMaxFrameSize())
			throw new IllegalArgumentException("Invalid settings, too big");

		ByteBuffer header = generateHeader(FrameType.SETTINGS, length, reply ? Flags.ACK : Flags.NONE, 0);

		for (Map.Entry<Integer, Integer> entry : settings.entrySet()) {
			header.putShort(entry.getKey().shortValue());
			header.putInt(entry.getValue());
		}

		BufferUtils.flipToFlush(header, 0);
		return header;
	}
}
