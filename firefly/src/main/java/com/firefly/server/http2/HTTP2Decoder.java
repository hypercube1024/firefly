package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class HTTP2Decoder implements Decoder {

	@Override
	public void decode(ByteBuffer buffer, Session session) throws Throwable {
		HTTP2SessionAttachment http2Session = (HTTP2SessionAttachment)session.getAttachment();
		http2Session.getServerParser().parse(buffer);;
	}

}
