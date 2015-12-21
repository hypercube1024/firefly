package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.DecoderChain;
import com.firefly.net.Session;

public class ServerSecureDecoder extends DecoderChain {

	public ServerSecureDecoder(DecoderChain next) {
		super(next);
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		HTTPConnection connection = (HTTPConnection) session.getAttachment();
		ByteBuffer plaintext;
		if(connection.getHttpVersion() == HttpVersion.HTTP_2) {
			plaintext = ((HTTP2ServerConnection)connection).getSSLSession().read(buf);
			if (plaintext != null && next != null)
				next.decode(plaintext, session);
		} else if(connection.getHttpVersion() == HttpVersion.HTTP_1_1) {
			// TODO
		}
	}

}
