package com.firefly.client.http2;

import com.firefly.codec.common.EncoderChain;
import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.Encoder;
import com.firefly.net.Session;

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
			if (message instanceof ByteBufferArrayOutputEntry) {
				ByteBufferArrayOutputEntry outputEntry = (ByteBufferArrayOutputEntry) message;
				session.write(outputEntry);
			}
		}
	}

}
