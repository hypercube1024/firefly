package com.firefly.server.http2;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.ByteBufferOutputEntry;
import com.firefly.net.EncoderChain;
import com.firefly.net.Session;

public class HTTP2ServerEncoder extends EncoderChain {

	public HTTP2ServerEncoder() {
	}

	public HTTP2ServerEncoder(EncoderChain next) {
		super(next);
	}

	@Override
	public void encode(Object message, Session session) throws Throwable {
		HTTPConnection connection = (HTTPConnection) session.getAttachment();
		if (connection.isEncrypted()) {
			next.encode(message, session);
		} else {
			if (message instanceof ByteBufferArrayOutputEntry) {
				session.write((ByteBufferArrayOutputEntry) message);
			} else if (message instanceof ByteBufferOutputEntry) {
				session.write((ByteBufferOutputEntry) message);
			}
		}
	}

}
