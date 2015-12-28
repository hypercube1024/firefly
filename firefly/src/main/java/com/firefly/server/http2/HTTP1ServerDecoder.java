package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.DecoderChain;
import com.firefly.net.Session;

public class HTTP1ServerDecoder extends DecoderChain {

	public HTTP1ServerDecoder(DecoderChain http2ServerDecoder) {
		super(http2ServerDecoder);
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		HTTPConnection connection = (HTTPConnection) session.getAttachment();

		switch (connection.getHttpVersion()) {
		case HTTP_2:
			next.decode(buf, session);
			break;
		case HTTP_1_1:
			HTTP1ServerConnection http1Connection = (HTTP1ServerConnection) session.getAttachment();
			while (buf.hasRemaining()) {
				http1Connection.getParser().parseNext(buf);
			}
			break;
		default:
			throw new IllegalStateException("server does not support the http version " + connection.getHttpVersion());
		}
	}

}
