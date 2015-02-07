package com.firefly.codec.spdy.frames.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;

public class GoAwayFrame extends ControlFrame {
	private final int lastStreamId;
	private final SessionStatus statusCode;

	public GoAwayFrame(short version, int lastStreamId, SessionStatus statusCode) {
		super(version, ControlFrameType.GO_AWAY, (byte) 0);
		this.lastStreamId = lastStreamId;
		this.statusCode = statusCode;
	}

	public int getLastStreamId() {
		return lastStreamId;
	}

	public SessionStatus getStatusCode() {
		return statusCode;
	}

	@Override
	public String toString() {
		return "GoAwayFrame [lastStreamId=" + lastStreamId + ", statusCode="
				+ statusCode + "]";
	}

	@Override
	public ByteBuffer toByteBuffer() {
		int frameBodyLength = 8;
        int totalLength = ControlFrame.HEADER_LENGTH + frameBodyLength;
        
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        generateControlFrameHeader(frameBodyLength, buffer);
        buffer.putInt(lastStreamId & 0x7F_FF_FF_FF);
        buffer.putInt(statusCode.getCode());
        buffer.flip();
        return buffer;
	}

}
