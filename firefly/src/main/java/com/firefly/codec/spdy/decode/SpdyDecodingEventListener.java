package com.firefly.codec.spdy.decode;

import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;
import com.firefly.net.Session;

public interface SpdyDecodingEventListener {
	
	public void onSynStream(SynStreamFrame synStreamFrame, Session session);
	
	public void onSynReply(SynReplyFrame synReplyFrame, Session session);
	
	public void onRstStream(RstStreamFrame rstStreamFrame, Session session);
	
	public void onSettings(SettingsFrame settingsFrame, Session session);
	
	public void onPing(PingFrame pingFrame, Session session);
	
	public void onGoAway(GoAwayFrame goAwayFrame, Session session);
	
	public void onHeaders(HeadersFrame headersFrame, Session session);
	
	public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame, Session session);
	
	public void onData(DataFrame dataFrame, Session session);
	
}
