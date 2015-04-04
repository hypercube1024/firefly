package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEvent;
import com.firefly.net.Session;

public class HeadersBodyParser extends AbstractParser {

	public HeadersBodyParser(SpdyDecodingEvent spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		if(isControlFrameUnderflow(buffer, session))
			return DecodeStatus.BUFFER_UNDERFLOW;
		
		return null;
	}

}
