package com.firefly.codec.spdy.frames.control;

import java.util.HashMap;
import java.util.Map;

import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;

public class RstStreamFrame extends ControlFrame {

	private final int streamId;
	private final ErrorCode statusCode;

	public RstStreamFrame(short version, int streamId, ErrorCode statusCode) {
		super(version, ControlFrameType.RST_STREAM, (byte) 0);
		this.streamId = streamId;
		this.statusCode = statusCode;
	}

	public int getStreamId() {
		return streamId;
	}

	public ErrorCode getStatusCode() {
		return statusCode;
	}
	
	@Override
	public String toString() {
		return "RstStreamFrame [streamId=" + streamId + ", statusCode="
				+ statusCode + "]";
	}

	public static enum ErrorCode {
		
		PROTOCOL_ERROR(1),
		INVALID_STREAM(2),
		REFUSED_STREAM(3),
		UNSUPPORTED_VERSION(4),
		CANCEL(5),
		INTERNAL_ERROR(6),
		FLOW_CONTROL_ERROR(7),
		STREAM_IN_USE(8),
		STREAM_ALREADY_CLOSED(9),
		FRAME_TOO_LARGE(11);
		
		public static ErrorCode from(int code) {
			return Codes.codes.get(code);
		}

		private final int code;

		private ErrorCode(int code) {
			this.code = code;
			Codes.codes.put(code, this);
		}

		public int code() {
			return code;
		}

		@Override
		public String toString() {
			return String.valueOf(code);
		}

		private static class Codes {
			private static final Map<Integer, ErrorCode> codes = new HashMap<>();
		}
	}
	
}
