package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEvent;
import com.firefly.codec.spdy.decode.exception.DecodingStateException;
import com.firefly.net.Session;

public class CredentialBodyParser extends AbstractParser {

	public CredentialBodyParser(SpdyDecodingEvent spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buf, Session session) {
		throw new DecodingStateException("SPDY v3.1 doesn't support Credential frame!");
	}

}
