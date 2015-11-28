package com.firefly.client.http2;

import com.firefly.codec.common.EncoderWrap;
import com.firefly.net.Encoder;
import com.firefly.net.Session;

public class HTTP2ClientEncoder extends EncoderWrap {
	
	public HTTP2ClientEncoder() {}
	
	public HTTP2ClientEncoder(Encoder next) {
		super(next);
	}

	@Override
	public void encode(Object message, Session session) throws Throwable {
		// TODO Auto-generated method stub

		if(next != null) {
			next.encode(message, session);
		}
	}

}
