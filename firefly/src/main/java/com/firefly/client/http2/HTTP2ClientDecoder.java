package com.firefly.client.http2;

import com.firefly.net.DecoderChain;
import com.firefly.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class HTTP2ClientDecoder extends DecoderChain {

	private static Logger log = LoggerFactory.getLogger("firefly-system");

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
