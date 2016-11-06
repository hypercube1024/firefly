package com.firefly.net.buffer;

import com.firefly.net.ReceiveBufferPool;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;

public class IOBufferPool implements ReceiveBufferPool {
	private static Logger log = LoggerFactory.getLogger("firefly-system");

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
