package com.firefly.codec.spdy.stream;

import com.firefly.codec.spdy.frames.control.PingFrame;

public interface PingEventListener {
	public void onPing(PingFrame pingFrame, Connection connection);
}
