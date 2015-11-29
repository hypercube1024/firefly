package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.common.EncoderChain;
import com.firefly.net.Encoder;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;

public class HTTP2ClientEncoder extends EncoderChain {

	public HTTP2ClientEncoder() {
	}

	public HTTP2ClientEncoder(Encoder next) {
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
