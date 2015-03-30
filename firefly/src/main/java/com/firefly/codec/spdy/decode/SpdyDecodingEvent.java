package com.firefly.codec.spdy.decode;

import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.Settings;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;

public interface SpdyDecodingEvent {
	
	public void onSynStream(SynStreamFrame synStreamFrame);
	
	public void onSynReply(SynReplyFrame synReplyFrame);
	
	public void onRstStream(RstStreamFrame rstStreamFrame);
	
	public void onSettings(Settings settings);
	
	public void onPing(PingFrame pingFrame);
	
	public void onGoAway(GoAwayFrame goAwayFrame);
	
	public void onHeaders(HeadersFrame headersFrame);
	
	public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame);
	
	public void onData(DataFrame dataFrame);
	
}
