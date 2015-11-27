package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Encoder;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;

public class SecureEncoder implements Encoder {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		HTTP2ClientConnection connection = (HTTP2ClientConnection) session.getAttachment();
		if (message instanceof ByteBuffer)
			connection.getSSLSession().write((ByteBuffer) message);
		else if (message instanceof FileRegion)
			connection.getSSLSession().transferFileRegion((FileRegion) message);
	}

}
