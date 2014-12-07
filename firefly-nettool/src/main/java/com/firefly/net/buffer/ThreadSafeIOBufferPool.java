package com.firefly.net.buffer;

import java.nio.ByteBuffer;

import com.firefly.net.ReceiveBufferPool;

public class ThreadSafeIOBufferPool implements ReceiveBufferPool {
	private final ThreadLocal<ReceiveBufferPool> receiveBufferPool = new ThreadLocal<ReceiveBufferPool>(){
		@Override
		protected ReceiveBufferPool initialValue() {
			return new SocketReceiveBufferPool();
		}
	};
	
	@Override
	public final ByteBuffer acquire(int size) {
		return receiveBufferPool.get().acquire(size);
	}
	
	@Override
	public final void release(ByteBuffer buffer) {
		receiveBufferPool.get().release(buffer);
	}
}
