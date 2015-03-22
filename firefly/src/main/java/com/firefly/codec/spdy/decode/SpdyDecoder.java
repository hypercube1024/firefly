package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class SpdyDecoder implements Decoder {

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		// TODO Auto-generated method stub

	}

	private enum DecodeStatus {
		NEXT, BUFFER_UNDERFLOW, INIT, COMPLETE, ERROR
	}
}
