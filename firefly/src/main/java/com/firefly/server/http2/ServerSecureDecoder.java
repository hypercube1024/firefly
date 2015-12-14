package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.common.DecoderChain;
import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class ServerSecureDecoder extends DecoderChain {

	public ServerSecureDecoder() {
	}

	public ServerSecureDecoder(Decoder next) {
		super(next);
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		HTTP2ServerConnection connection = (HTTP2ServerConnection) session.getAttachment();
		ByteBuffer plaintext = connection.getSSLSession().read(buf);
		if (plaintext != null && next != null)
			next.decode(plaintext, session);
	}

}
