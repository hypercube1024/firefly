package com.firefly.client.http2;

import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.Encoder;
import com.firefly.net.Session;

public class ClientSecureEncoder implements Encoder {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		HTTP2ClientConnection connection = (HTTP2ClientConnection) session.getAttachment();
		if (message instanceof ByteBufferArrayOutputEntry) {
			ByteBufferArrayOutputEntry outputEntry = (ByteBufferArrayOutputEntry) message;
			connection.getSSLSession().write(outputEntry.getData(), outputEntry.getCallback());
		}
	}

}
