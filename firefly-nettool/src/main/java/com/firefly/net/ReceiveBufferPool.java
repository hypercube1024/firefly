package com.firefly.net;

import java.nio.ByteBuffer;

public interface ReceiveBufferPool {

	/**
	 * Get byte buffer
	 * @param size 
	 * 		buffer size
	 * @return Byte buffer
	 */
	ByteBuffer acquire(int size);

	/**
	 * Return buffer to the pool
	 * @param buffer
	 * 		Byte buffer
	 */
	void release(ByteBuffer buffer);

}