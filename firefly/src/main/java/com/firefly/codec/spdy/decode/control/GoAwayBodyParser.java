package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEvent;
import com.firefly.net.Session;

public class GoAwayBodyParser extends AbstractParser {

	public GoAwayBodyParser(SpdyDecodingEvent spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buf, Session session) {
		// TODO Auto-generated method stub
		return null;
	}

}
