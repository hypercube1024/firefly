package com.firefly.codec.common;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class DecoderChain implements Decoder {

	protected volatile Decoder next;
	
	public DecoderChain() {}
	
	public DecoderChain(Decoder next) {
		this.next = next;
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		next.decode(buf, session);
	}

	public Decoder getNext() {
		return next;
	}

	public void setNext(Decoder next) {
		this.next = next;
	}

}
