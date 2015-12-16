package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ClientDecoder implements Decoder {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	@Override
	public void decode(ByteBuffer buffer, Session session) throws Throwable {
		if (!buffer.hasRemaining())
			return;

		ByteBuffer newBuffer = BufferUtils.copy(buffer);
		if (log.isDebugEnable())
			log.debug("client receives the data {}, {}", newBuffer.remaining(), newBuffer.hasRemaining());

		HTTP2ClientConnection http2ClientConnection = (HTTP2ClientConnection) session.getAttachment();
		// TODO convert direct buffer to heap buffer.  optimize it ?
		http2ClientConnection.getParser().parse(newBuffer);
	}

}
