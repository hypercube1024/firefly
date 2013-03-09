package com.firefly.server.http;

import java.nio.ByteBuffer;

import com.firefly.net.Encoder;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;

public class HttpEncoder implements Encoder {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		if (message instanceof ByteBuffer)
			session.write((ByteBuffer) message);
		else if (message instanceof FileRegion)
			session.write((FileRegion) message);
	}

}
