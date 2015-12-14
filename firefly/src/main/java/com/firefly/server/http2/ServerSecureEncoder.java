package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Encoder;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;

public class ServerSecureEncoder implements Encoder {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		HTTP2ServerConnection connection = (HTTP2ServerConnection) session.getAttachment();
		if (message instanceof ByteBuffer)
			connection.getSSLSession().write((ByteBuffer) message);
		else if (message instanceof FileRegion)
			connection.getSSLSession().transferFileRegion((FileRegion) message);
	}

}
