package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class HTTP2ClientDecoder implements Decoder {

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		HTTP2ClientSessionAttachment attachment = (HTTP2ClientSessionAttachment) session.getAttachment();
		attachment.getParser().parse(buf);
	}

}
