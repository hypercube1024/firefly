package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class HTTP2ServerDecoder implements Decoder {

	@Override
	public void decode(ByteBuffer buffer, Session session) throws Throwable {
		HTTP2ServerConnection connection = (HTTP2ServerConnection) session.getAttachment();
		connection.getParser().parse(buffer);;
	}

}
