package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.net.Session;

public class DataFrameParser extends AbstractParser {

	public DataFrameParser(SpdyDecodingEvent spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buf, Session session) {
		// TODO Auto-generated method stub
		return null;
	}

}
