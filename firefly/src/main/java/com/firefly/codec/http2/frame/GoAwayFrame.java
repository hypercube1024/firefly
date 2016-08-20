package com.firefly.codec.http2.frame;

import java.nio.charset.StandardCharsets;

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
		if (payload == null || payload.length == 0)
			return "";
		try {
			return new String(payload, StandardCharsets.UTF_8);
		} catch (Throwable x) {
			return "";
		}
	}

	@Override
	public String toString() {
		ErrorCode errorCode = ErrorCode.from(error);
		return String.format("%s,%d/%s/%s", super.toString(), lastStreamId,
				errorCode != null ? errorCode.toString() : String.valueOf(error), tryConvertPayload());
	}
}
