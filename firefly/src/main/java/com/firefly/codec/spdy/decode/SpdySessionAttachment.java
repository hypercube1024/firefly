package com.firefly.codec.spdy.decode;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.control.ControlFrameHeader;
import com.firefly.codec.spdy.decode.control.HeadersBlockParser;
import com.firefly.codec.spdy.frames.control.HeadersBlockGenerator;
import com.firefly.net.tcp.ssl.SSLSession;

public class SpdySessionAttachment implements Closeable {
	
	public HeadersBlockParser headersBlockParser = HeadersBlockParser.newInstance();
	public HeadersBlockGenerator headersBlockGenerator = HeadersBlockGenerator.newInstance();
	public SSLSession sslSession;
	public ByteBuffer byteBuffer;
	
	// decoding state
	public FrameType frameType;
	public ControlFrameParserState controlFrameParserState = ControlFrameParserState.HEAD;
	public ControlFrameHeader controlFrameHeader;
	
	public void reset() {
		this.byteBuffer = null;
		this.frameType = null;
		this.controlFrameParserState = ControlFrameParserState.HEAD;
		this.controlFrameHeader = null;
	}
	
	public boolean isInitialized() {
		return byteBuffer == null 
				&& frameType == null
				&& controlFrameParserState == ControlFrameParserState.HEAD
				&& controlFrameHeader == null;
	}

	@Override
	public void close() throws IOException {
		reset();
		if(sslSession != null) {
			sslSession.close();
		}
		headersBlockGenerator.close();
		headersBlockParser.close();
	}
}
