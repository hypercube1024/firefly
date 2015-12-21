package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.net.DecoderChain;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ServerDecoder extends DecoderChain {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	@Override
	public void decode(ByteBuffer buffer, Session session) throws Throwable {
		if(!buffer.hasArray()) {
			throw new IllegalArgumentException("the byte buffer has not array");
		}
		
		if(!buffer.hasRemaining())
			return;

		if(log.isDebugEnabled())
			log.debug("the server session {} received the {} bytes", session.getSessionId(), buffer.remaining());
		
		HTTP2ServerConnection connection = (HTTP2ServerConnection) session.getAttachment();
		connection.getParser().parse(buffer);
	}

}
