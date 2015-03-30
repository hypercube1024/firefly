package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.control.ControlFrameHeader;
import com.firefly.net.tcp.ssl.SSLSession;

public class SpdySessionAttachment {
	public SSLSession sslSession;
	public ByteBuffer byteBuffer;
	
	// decoding state
	public FrameType frameType;
	public ControlFrameParserState controlFrameParserState = ControlFrameParserState.HEAD;
	
	public ControlFrameHeader controlFrameHeader;
}
