package com.firefly.net;

import java.nio.ByteBuffer;

public interface ReceiveBufferPool {

	/**
	 * 从池中获取指定容量的ByteBuffer
	 * @param size buffer的容量
	 * @return ByteBuffer
	 */
	ByteBuffer acquire(int size);

	/**
	 * 将一个ByteBuffer返回到池中
	 * @param buffer
	 */
	void release(ByteBuffer buffer);

}