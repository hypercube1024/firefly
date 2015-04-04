package com.firefly.codec.spdy.frames.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;

public class RstStreamFrame extends ControlFrame {

	private final int streamId;
	private final StreamErrorCode statusCode;

	public RstStreamFrame(short version, int streamId, StreamErrorCode statusCode) {
		super(version, ControlFrameType.RST_STREAM, (byte) 0);
		this.streamId = streamId;
		this.statusCode = statusCode;
	}

	public int getStreamId() {
		return streamId;
	}

	public StreamErrorCode getStatusCode() {
		return statusCode;
	}
	
	@Override
	public String toString() {
		return "RstStreamFrame [streamId=" + streamId + ", statusCode="
				+ statusCode + "]";
	}

	public static enum StreamErrorCode {
		
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
		
		public static StreamErrorCode from(int code) {
			return Codes.codes[code - 1];
		}

		private final int code;

		private StreamErrorCode(int code) {
			this.code = code;
			Codes.codes[code - 1] = this;
		}

		public int code() {
			return code;
		}

		@Override
		public String toString() {
			return String.valueOf(code);
		}

		private static class Codes {
			private static final StreamErrorCode[] codes = new StreamErrorCode[12];
		}
	}

	@Override
	public ByteBuffer toByteBuffer() {
		int frameBodyLength = 8;
		int totalLength = ControlFrame.HEADER_LENGTH + frameBodyLength;
		
		ByteBuffer buffer = ByteBuffer.allocate(totalLength);
		generateControlFrameHeader(frameBodyLength, buffer);
		buffer.putInt(streamId & 0x7F_FF_FF_FF);
        buffer.putInt(statusCode.code());
        buffer.flip();
        return buffer;
	}
	
}
