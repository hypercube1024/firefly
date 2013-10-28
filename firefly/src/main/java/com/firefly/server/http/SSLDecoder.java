package com.firefly.server.http;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class SSLDecoder implements Decoder {
	
	private final HttpDecoder next;

	public SSLDecoder(HttpDecoder next) {
		this.next = next;
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		SessionAttachment sessionAttachment = (SessionAttachment)session.getAttachment();
		ByteBuffer plaintext = sessionAttachment.sslSession.read(buf);
		if(plaintext != null)
			next.decode(plaintext, session);
	}

}
