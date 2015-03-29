package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.net.Session;

public interface Parser {
	
	public DecodeStatus parse(ByteBuffer buf, Session session);
	
}
