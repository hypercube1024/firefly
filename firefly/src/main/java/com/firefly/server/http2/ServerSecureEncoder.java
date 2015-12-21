package com.firefly.server.http2;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.ByteBufferOutputEntry;
import com.firefly.net.EncoderChain;
import com.firefly.net.Session;

public class ServerSecureEncoder extends EncoderChain {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		HTTPConnection connection = (HTTPConnection) session.getAttachment();
		if (message instanceof ByteBufferArrayOutputEntry) {
			ByteBufferArrayOutputEntry outputEntry = (ByteBufferArrayOutputEntry) message;
			if (connection.getHttpVersion() == HttpVersion.HTTP_2)
				((HTTP2ServerConnection) connection).getSSLSession().write(outputEntry.getData(),
						outputEntry.getCallback());
			else if (connection.getHttpVersion() == HttpVersion.HTTP_1_1) {
				// TODO
			}
		} else if (message instanceof ByteBufferOutputEntry) {
			ByteBufferOutputEntry outputEntry = (ByteBufferOutputEntry) message;
			if (connection.getHttpVersion() == HttpVersion.HTTP_2)
				((HTTP2ServerConnection) connection).getSSLSession().write(outputEntry.getData(),
						outputEntry.getCallback());
			else if (connection.getHttpVersion() == HttpVersion.HTTP_1_1) {
				// TODO
			}
		}
	}

}
