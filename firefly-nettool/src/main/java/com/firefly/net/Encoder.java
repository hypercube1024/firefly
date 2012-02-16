package com.firefly.net;

public interface Encoder {
	void encode(Object message, Session session) throws Throwable;
}
