package com.firefly.codec.spdy.stream;

import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;

public interface StreamEventListener  {

	public void onSynStream(SynStreamFrame synStreamFrame, Stream stream, Connection connection);
	
	public void onSynReply(SynReplyFrame synReplyFrame, Stream stream, Connection connection);
	
	public void onHeaders(HeadersFrame headersFrame, Stream stream, Connection connection);
	
	public void onRstStream(RstStreamFrame rstStreamFrame, Stream stream, Connection connection);
	
	public void onData(DataFrame dataFrame, Stream stream, Connection connection);
	
	public void onGoAway(GoAwayFrame goAwayFrame, Stream stream, Connection connection);

}
