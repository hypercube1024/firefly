package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ClientDecoder implements Decoder {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	@Override
	public void decode(ByteBuffer buffer, Session session) throws Throwable {
		if(!buffer.hasArray()) {
			throw new IllegalArgumentException("the byte buffer has not array");
		}
		
		if (!buffer.hasRemaining())
			return;

		if (log.isDebugEnabled())
			log.debug("the client session {} received the {} bytes", session.getSessionId(), buffer.remaining());

		HTTP2ClientConnection http2ClientConnection = (HTTP2ClientConnection) session.getAttachment();
		http2ClientConnection.getParser().parse(buffer);
	}

}
