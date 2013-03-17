package com.firefly.server.http;

import java.nio.ByteBuffer;

import com.firefly.net.Encoder;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.net.tcp.ssl.SSLSession;

public class SSLEncoder implements Encoder {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		if(message != null) {
			SSLSession c = (SSLSession)session.getAttribute(HttpHandler.SSL_SESSION_KEY);
			if (message instanceof ByteBuffer)
				c.write((ByteBuffer) message);
			else if (message instanceof FileRegion)
				c.transferFileRegion((FileRegion) message);
		}
	}

}
