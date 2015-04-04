package com.firefly.codec.spdy.frames.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;

public class WindowUpdateFrame extends ControlFrame {
	private final int streamId;
	private final int windowDelta;

	public WindowUpdateFrame(short version, int streamId, int windowDelta) {
		super(version, ControlFrameType.WINDOW_UPDATE, (byte) 0);
		this.streamId = streamId;
		this.windowDelta = windowDelta;
	}

	public int getStreamId() {
		return streamId;
	}

	public int getWindowDelta() {
		return windowDelta;
	}

	@Override
	public String toString() {
		return "WindowUpdateFrame [streamId=" + streamId + ", windowDelta="
				+ windowDelta + ", toString()=" + super.toString() + "]";
	}

	@Override
	public ByteBuffer toByteBuffer() {
		int frameBodyLength = 8;
        int totalLength = ControlFrame.HEADER_LENGTH + frameBodyLength;
        
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        generateControlFrameHeader(frameBodyLength, buffer);
        buffer.putInt(streamId & 0x7F_FF_FF_FF);
        buffer.putInt(windowDelta & 0x7F_FF_FF_FF);
        buffer.flip();
        return buffer;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + streamId;
		result = prime * result + windowDelta;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		WindowUpdateFrame other = (WindowUpdateFrame) obj;
		if (streamId != other.streamId)
			return false;
		if (windowDelta != other.windowDelta)
			return false;
		return true;
	}

}
