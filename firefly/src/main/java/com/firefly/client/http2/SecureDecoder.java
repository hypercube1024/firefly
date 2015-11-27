package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class SecureDecoder implements Decoder {

	private volatile Decoder next;

	public SecureDecoder() {
	}

	public SecureDecoder(Decoder next) {
		this.next = next;
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		HTTP2ClientConnection connection = (HTTP2ClientConnection) session.getAttachment();
		ByteBuffer plaintext = connection.getSSLSession().read(buf);
		if (plaintext != null)
			next.decode(plaintext, session);
	}

	public Decoder getNext() {
		return next;
	}

	public void setNext(Decoder next) {
		this.next = next;
	}

}
