package com.firefly.codec.spdy.encode;

import com.firefly.codec.spdy.frames.Serialization;
import com.firefly.net.Encoder;
import com.firefly.net.Session;

public class SpdyEncoder implements Encoder {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		if(message instanceof Serialization) {
			Serialization serialization = (Serialization) message;
			session.write(serialization.toByteBuffer());
		}
	}

}
