package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ServerDecoder implements Decoder {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	@Override
	public void decode(ByteBuffer buffer, Session session) throws Throwable {
		if(!buffer.hasRemaining())
			return;

		if(log.isDebugEnable())
			log.debug("server receives the data {}", buffer.remaining());
		
		HTTP2ServerConnection connection = (HTTP2ServerConnection) session.getAttachment();
		// TODO convert direct buffer to heap buffer.  optimize it ?
		connection.getParser().parse(ByteBuffer.wrap(BufferUtils.toArray(buffer)));
	}

}
