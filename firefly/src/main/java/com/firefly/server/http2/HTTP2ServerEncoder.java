package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.common.EncoderChain;
import com.firefly.net.Encoder;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;

public class HTTP2ServerEncoder extends EncoderChain {

	public HTTP2ServerEncoder() {
	}

	public HTTP2ServerEncoder(Encoder next) {
		super(next);
	}

	@Override
	public void encode(Object message, Session session) throws Throwable {
		if (next != null) {
			next.encode(message, session);
		} else {
			if (message instanceof ByteBuffer)
				session.write((ByteBuffer) message);
			else if (message instanceof FileRegion)
				session.write((FileRegion) message);
		}
	}

}
