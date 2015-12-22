package com.firefly.server.http2;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.EncoderChain;
import com.firefly.net.Session;

public class HTTP1ServerEncoder extends EncoderChain {

	public HTTP1ServerEncoder(EncoderChain next) {
		super(next);
	}

	@Override
	public void encode(Object message, Session session) throws Throwable {
		HTTPConnection connection = (HTTPConnection) session.getAttachment();
		if (connection.getHttpVersion() == HttpVersion.HTTP_2) {
			next.encode(message, session);
		} else if (connection.getHttpVersion() == HttpVersion.HTTP_1_1) {
			// TODO http1 generator
			if(connection.isEncrypted()) {
				next.encode(message, session);
			} else {
				
			}
		}
	}

}
