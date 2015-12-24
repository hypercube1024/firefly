package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.ByteBufferOutputEntry;
import com.firefly.net.EncoderChain;
import com.firefly.net.Session;

public class ClientSecureEncoder extends EncoderChain {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		HTTPConnection connection = (HTTPConnection) session.getAttachment();

		switch (connection.getHttpVersion()) {
		case HTTP_2: {
			HTTP2ClientConnection http2ClientConnection = (HTTP2ClientConnection) connection;
			if (message instanceof ByteBufferArrayOutputEntry) {
				ByteBufferArrayOutputEntry outputEntry = (ByteBufferArrayOutputEntry) message;
				http2ClientConnection.getSSLSession().write(outputEntry.getData(), outputEntry.getCallback());
			} else if (message instanceof ByteBufferOutputEntry) {
				ByteBufferOutputEntry outputEntry = (ByteBufferOutputEntry) message;
				http2ClientConnection.getSSLSession().write(outputEntry.getData(), outputEntry.getCallback());
			}
		}
			break;
		case HTTP_1_1:
			// TODO not implements
			break;
		default:
			throw new IllegalStateException("client does not support the http version " + connection.getHttpVersion());
		}
	}

}
