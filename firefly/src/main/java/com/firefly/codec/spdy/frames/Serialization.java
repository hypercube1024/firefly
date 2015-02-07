package com.firefly.codec.spdy.frames;

import java.nio.ByteBuffer;

public interface Serialization {
	public ByteBuffer toByteBuffer();
}
