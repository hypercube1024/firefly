package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.DecoderChain;
import com.firefly.net.Session;

public class HTTP1ClientDecoder extends DecoderChain {

	public HTTP1ClientDecoder(DecoderChain next) {
		super(next);
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		HTTPConnection connection = (HTTPConnection) session.getAttachment();

		switch (connection.getHttpVersion()) {
		case HTTP_2:
			next.decode(buf, session);
			break;
		case HTTP_1_1:
			HTTP1ClientConnection http1Connection = (HTTP1ClientConnection) session.getAttachment();
			while(buf.hasRemaining()) {
				http1Connection.getParser().parseNext(buf);
			}
			break;
		default:
			throw new IllegalStateException("client does not support the http version " + connection.getHttpVersion());
		}
	}

}
