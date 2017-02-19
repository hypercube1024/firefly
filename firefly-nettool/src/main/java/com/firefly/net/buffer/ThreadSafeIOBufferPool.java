package com.firefly.net.buffer;

import com.firefly.net.BufferPool;

import java.nio.ByteBuffer;

public class ThreadSafeIOBufferPool implements BufferPool {

    private final ThreadLocal<BufferPool> safeBufferPool;

    public ThreadSafeIOBufferPool() {
        this(true);
    }

    public ThreadSafeIOBufferPool(boolean directBuffer) {
        safeBufferPool = ThreadLocal.withInitial(() -> new IOBufferPool(directBuffer));
    }

    @Override
    public final ByteBuffer acquire(int size) {
        return safeBufferPool.get().acquire(size);
    }

    @Override
    public final void release(ByteBuffer buffer) {
        safeBufferPool.get().release(buffer);
    }

    @Override
    public int size() {
        return safeBufferPool.get().size();
    }
}
