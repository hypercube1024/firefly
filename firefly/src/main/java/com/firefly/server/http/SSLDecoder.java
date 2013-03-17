package com.firefly.server.http;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

public class SSLDecoder implements Decoder {
	
	private final HttpDecoder next;

	public SSLDecoder(HttpDecoder next) {
		this.next = next;
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		SSLSession c = (SSLSession)session.getAttribute(HttpHandler.SSL_SESSION_KEY);

		ByteBuffer plaintext = c.read(buf);
		if(plaintext != null)
			next.decode(plaintext, session);
	}

}
