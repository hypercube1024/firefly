package com.firefly.codec.spdy.stream;

import com.firefly.codec.spdy.decode.SpdyDecodingEventListener;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;
import com.firefly.net.Session;

public abstract class StreamEventListener implements SpdyDecodingEventListener {

	@Override
	public void onSettings(SettingsFrame settingsFrame, Session session) {}

	@Override
	public void onPing(PingFrame pingFrame, Session session) {}

	@Override
	public void onGoAway(GoAwayFrame goAwayFrame, Session session) {}
	
	@Override
	public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame, Session session) {
		
	}

}
