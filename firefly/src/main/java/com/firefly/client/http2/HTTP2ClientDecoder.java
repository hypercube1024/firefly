package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ClientDecoder implements Decoder {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		if (!buf.hasRemaining())
			return;

		if (log.isDebugEnable())
			log.debug("client receives the data {}, {}", buf.remaining(), buf.hasRemaining());

		HTTP2ClientConnection http2ClientConnection = (HTTP2ClientConnection) session.getAttachment();
		http2ClientConnection.getParser().parse(buf);
	}

}
