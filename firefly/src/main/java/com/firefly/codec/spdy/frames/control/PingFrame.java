package com.firefly.codec.spdy.frames.control;

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
		return "PingFrame [pingId=" + pingId + "]";
	}

}
