package com.firefly.codec.http2.frame;

import java.nio.ByteBuffer;

import com.firefly.utils.io.BufferUtils;

public class GoAwayFrame extends Frame {
	private final int lastStreamId;
	private final int error;
	private final byte[] payload;

	public GoAwayFrame(int lastStreamId, int error, byte[] payload) {
		super(FrameType.GO_AWAY);
		this.lastStreamId = lastStreamId;
		this.error = error;
		this.payload = payload;
	}

	public int getLastStreamId() {
		return lastStreamId;
	}

	public int getError() {
		return error;
	}

	public byte[] getPayload() {
		return payload;
	}

	public String tryConvertPayload() {
		if (payload == null)
			return "";
		ByteBuffer buffer = BufferUtils.toBuffer(payload);
		try {
			return BufferUtils.toUTF8String(buffer);
		} catch (Throwable x) {
			return BufferUtils.toDetailString(buffer);
		}
	}

	@Override
	public String toString() {
		return String.format("%s,%d/%s", super.toString(), error, tryConvertPayload());
	}
}
