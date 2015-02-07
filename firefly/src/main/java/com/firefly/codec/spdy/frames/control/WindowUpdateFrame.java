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
				+ windowDelta + "]";
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

}
