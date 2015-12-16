package com.firefly.server.http2;

import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.Encoder;
import com.firefly.net.Session;

public class ServerSecureEncoder implements Encoder {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		HTTP2ServerConnection connection = (HTTP2ServerConnection) session.getAttachment();
		if (message instanceof ByteBufferArrayOutputEntry) {
			ByteBufferArrayOutputEntry outputEntry = (ByteBufferArrayOutputEntry) message;
			connection.getSSLSession().write(outputEntry.getData(), outputEntry.getCallback());
		}
	}

}
