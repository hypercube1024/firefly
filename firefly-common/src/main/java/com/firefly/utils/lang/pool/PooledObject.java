package com.firefly.utils.lang.pool;

import com.firefly.utils.function.Action1;
import com.firefly.utils.time.Millisecond100Clock;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pengtao Qiu
 */
public class PooledObject<T> implements Closeable {

    protected final Pool<T> pool;
    protected final T object;
    protected final long createTime;
    protected long activeTime;
    protected final Action1<PooledObject<T>> leakCallback;
    protected final AtomicBoolean released = new AtomicBoolean(false);

    public PooledObject(T object, Pool<T> pool, Action1<PooledObject<T>> leakCallback) {
        this.object = object;
        this.pool = pool;
        this.leakCallback = leakCallback;
        createTime = Millisecond100Clock.currentTimeMillis();
        activeTime = createTime;
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

    /**
     * Clear leak track
     */
    public void clear() {
        pool.getLeakDetector().clear(this);
    }

    /**
     * Register leak track
     */
    public void register() {
        pool.getLeakDetector().register(this, leakCallback);
    }

    public AtomicBoolean getReleased() {
        return released;
    }

    @Override
    public String toString() {
        return "PooledObject{" +
                "object=" + object +
                ", createTime=" + createTime +
                ", activeTime=" + activeTime +
                '}';
    }

    @Override
    public void close() {
        release();
    }
}
