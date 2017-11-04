package com.firefly.utils.lang.pool;

import com.firefly.utils.time.Millisecond100Clock;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pengtao Qiu
 */
public class PooledObject<T> {

    private final Pool<T> pool;
    private final T object;
    private final long createTime;
    private long activeTime;
    private AtomicBoolean released = new AtomicBoolean(false);

    public PooledObject(T object, Pool<T> pool) {
        this.object = object;
        this.pool = pool;
        createTime = Millisecond100Clock.currentTimeMillis();
        activeTime = createTime;
    }

    /**
     * Check the pooled object has been taken.
     *
     * @return If return true, the pooled object has not been taken, or else The other thread has taken the pooled object.
     */
    boolean prepareTake() {
        return released.compareAndSet(true, false);
    }

    /**
     * Check the pooled object has been released.
     *
     * @return If return true, the pooled object has not been released, or else The other thread has released the pooled object.
     */
    boolean prepareRelease() {
        return released.compareAndSet(false, true);
    }

    /**
     * If return true the pooled object is released.
     *
     * @return If return true the pooled object is released.
     */
    public boolean isReleased() {
        return released.get();
    }

    /**
     * Get the cached object.
     *
     * @return T the cached object.
     */
    public T getObject() {
        activeTime = Millisecond100Clock.currentTimeMillis();
        return object;
    }

    /**
     * The pooled object created time.
     *
     * @return The pooled object created time.
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     * The last time to get the object.
     *
     * @return The last time to get the object.
     */
    public long getActiveTime() {
        return activeTime;
    }

    /**
     * Return the pooled object to the pool.
     */
    public void release() {
        pool.release(this);
    }
}
