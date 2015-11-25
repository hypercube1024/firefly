package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class HTTP2ClientDecoder implements Decoder {

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		HTTP2ClientConnection http2ClientConnection = (HTTP2ClientConnection) session.getAttachment();
		http2ClientConnection.getParser().parse(buf);
	}

}
