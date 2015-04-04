package com.firefly.codec.spdy.frames.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;

public class PingFrame extends ControlFrame {
	
	private final int pingId;

	public PingFrame(short version, int pingId) {
		super(version, ControlFrameType.PING, (byte) 0);
		this.pingId = pingId;
	}

	public int getPingId() {
		return pingId;
	}
	
	@Override
	public String toString() {
		return "PingFrame [pingId=" + pingId + ", toString()="
				+ super.toString() + "]";
	}

	@Override
	public ByteBuffer toByteBuffer() {
		int frameBodyLength = 4;
        int totalLength = ControlFrame.HEADER_LENGTH + frameBodyLength;
        
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        generateControlFrameHeader(frameBodyLength, buffer);
        buffer.putInt(pingId);
        buffer.flip();
        return buffer;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + pingId;
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
		PingFrame other = (PingFrame) obj;
		if (pingId != other.pingId)
			return false;
		return true;
	}

}
