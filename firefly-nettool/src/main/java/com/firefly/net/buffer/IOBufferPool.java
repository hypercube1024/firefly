package com.firefly.net.buffer;

import com.firefly.net.BufferPool;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;

public class IOBufferPool implements BufferPool {
    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private static final int POOL_SIZE = 8;

    @SuppressWarnings("unchecked")
    private final SoftReference<ByteBuffer>[] pool = new SoftReference[POOL_SIZE];
    private final boolean directBuffer;

    public IOBufferPool() {
        this(true);
    }

    public IOBufferPool(boolean directBuffer) {
        this.directBuffer = directBuffer;
    }

    @Override
    public ByteBuffer acquire(int size) {
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

            if (buf.capacity() >= size) {
                pool[i] = null;
                buf.clear();
                return buf;
            }
        }

        int allocateSize = BufferUtils.normalizeBufferSize(size);
        log.debug("acquire buffer size: {}", allocateSize);
        return directBuffer ? ByteBuffer.allocateDirect(allocateSize) : ByteBuffer.allocate(allocateSize);
    }

    @Override
    public void release(ByteBuffer buffer) {
        for (int i = 0; i < POOL_SIZE; i++) {
            SoftReference<ByteBuffer> ref = pool[i];
            if (ref == null || ref.get() == null) {
                pool[i] = new SoftReference<>(buffer);
                return;
            }
        }

        // pool is full - replace one
        final int capacity = buffer.capacity();
        for (int i = 0; i < POOL_SIZE; i++) {
            SoftReference<ByteBuffer> ref = pool[i];
            if (ref == null) {
                pool[i] = new SoftReference<>(buffer);
                return;
            } else {
                ByteBuffer pooled = ref.get();
                if (pooled == null) {
                    pool[i] = new SoftReference<>(buffer);
                    return;
                } else {
                    if (pooled.capacity() < capacity) {
                        pool[i] = new SoftReference<>(buffer);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public int size() {
        int count = 0;
        for (int i = 0; i < POOL_SIZE; i++) {
            SoftReference<ByteBuffer> ref = pool[i];
            if (ref != null && ref.get() != null) {
                count++;
            }
        }
        return count;
    }

}
