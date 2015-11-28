package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.common.DecoderWrap;
import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class SecureDecoder extends DecoderWrap {

	public SecureDecoder() {
	}

	public SecureDecoder(Decoder next) {
		super(next);
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		HTTP2ClientConnection connection = (HTTP2ClientConnection) session.getAttachment();
		ByteBuffer plaintext = connection.getSSLSession().read(buf);
		if (plaintext != null && next != null)
			next.decode(plaintext, session);
	}

}
