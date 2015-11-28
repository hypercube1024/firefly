package com.firefly.codec.common;

import com.firefly.net.Encoder;
import com.firefly.net.Session;

public class EncoderWrap implements Encoder {
	
	protected volatile Encoder next;
	
	public EncoderWrap() {}

	public EncoderWrap(Encoder next) {
		this.next = next;
	}

	@Override
	public void encode(Object message, Session session) throws Throwable {
		next.encode(message, session);
	}

	public Encoder getNext() {
		return next;
	}

	public void setNext(Encoder next) {
		this.next = next;
	}

	
}
