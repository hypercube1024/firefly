package com.firefly.net.buffer;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;

import com.firefly.net.ReceiveBufferPool;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class IOBufferPool implements ReceiveBufferPool {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private static final int POOL_SIZE = 8;

	@SuppressWarnings("unchecked")
	private final SoftReference<ByteBuffer>[] pool = new SoftReference[POOL_SIZE];

	@Override
	public final ByteBuffer acquire(int size) {
		final SoftReference<ByteBuffer>[] pool = this.pool;
		for (int i = 0; i < POOL_SIZE; i++) {
			SoftReference<ByteBuffer> ref = pool[i];
			if (ref == null) {
				continue;
			}

			ByteBuffer buf = ref.get();
			if (buf == null) {
				pool[i] = null;
				continue;
			}

			if (buf.capacity() < size) {
				continue;
			}

			pool[i] = null;

			buf.clear();
			return buf;
		}
		
		int allocateSize = BufferUtils.normalizeBufferSize(size);
		log.debug("acquire read size: {}", allocateSize);

		ByteBuffer buf = ByteBuffer.allocateDirect(allocateSize);
		buf.clear();
		return buf;
	}

	@Override
	public final void release(ByteBuffer buffer) {
		final SoftReference<ByteBuffer>[] pool = this.pool;
		for (int i = 0; i < POOL_SIZE; i++) {
			SoftReference<ByteBuffer> ref = pool[i];
			if (ref == null || ref.get() == null) {
				pool[i] = new SoftReference<ByteBuffer>(buffer);
				return;
			}
		}

		// pool is full - replace one
		final int capacity = buffer.capacity();
		for (int i = 0; i < POOL_SIZE; i++) {
			SoftReference<ByteBuffer> ref = pool[i];
			ByteBuffer pooled = ref.get();
			if (pooled == null) {
				pool[i] = null;
				continue;
			}

			if (pooled.capacity() < capacity) {
				pool[i] = new SoftReference<ByteBuffer>(buffer);
				return;
			}
		}
	}

}
