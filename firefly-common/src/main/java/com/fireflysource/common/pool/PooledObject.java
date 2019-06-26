package com.fireflysource.common.pool;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public class PooledObject<T> implements Closeable {

    protected final Pool<T> pool;
    protected final T object;
    protected final Consumer<PooledObject<T>> leakCallback;
    final AtomicBoolean released = new AtomicBoolean(false);

    public PooledObject(T object, Pool<T> pool, Consumer<PooledObject<T>> leakCallback) {
        this.object = object;
        this.pool = pool;
        this.leakCallback = leakCallback;
    }

    /**
     * Get the cached object.
     *
     * @return T the cached object.
     */
    public T getObject() {
        return object;
    }

    /**
     * Return the pooled object to the pool.
     */
    public void release() {
        pool.release(this);
    }

    public boolean isReleased() {
        return released.get();
    }

    @Override
    public void close() {
        release();
    }
}
