package com.fireflysource.common.pool;

import com.fireflysource.common.io.AsyncCloseable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public class PooledObject<T> implements AsyncCloseable {

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

    public boolean isReleased() {
        return released.get();
    }

    @Override
    public CompletableFuture<Void> closeFuture() {
        return pool.release(this);
    }

    @Override
    public void close() {
        closeFuture();
    }

    @Override
    public String toString() {
        return "PooledObject{" +
                "object=" + object +
                ", released=" + released +
                '}';
    }


}
