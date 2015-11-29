package com.firefly.codec.common;

import com.firefly.net.Encoder;
import com.firefly.net.Session;

public class EncoderChain implements Encoder {
	
	protected volatile Encoder next;
	
	public EncoderChain() {}

	public EncoderChain(Encoder next) {
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
