package com.firefly.codec.http2.frame;

public class ResetFrame extends Frame {
	private final int streamId;
	private final int error;

	public ResetFrame(int streamId, int error) {
		super(FrameType.RST_STREAM);
		this.streamId = streamId;
		this.error = error;
	}

	public int getStreamId() {
		return streamId;
	}

	public int getError() {
		return error;
	}

	@Override
	public String toString() {
		ErrorCode errorCode = ErrorCode.from(error);
		String reason = errorCode == null ? "error=" + error : errorCode.name().toLowerCase();
		return String.format("%s#%d{%s}", super.toString(), streamId, reason);
	}
}
