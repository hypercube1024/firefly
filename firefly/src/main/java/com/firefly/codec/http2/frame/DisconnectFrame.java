package com.firefly.codec.http2.frame;

public class DisconnectFrame extends Frame {
	public DisconnectFrame() {
		super(FrameType.DISCONNECT);
	}
}
