package com.firefly.utils.lang.pool;

import com.firefly.utils.time.Millisecond100Clock;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pengtao Qiu
 */
public class PooledObject<T> {

    private final T object;
    private final long createTime;
    private long activeTime;
    private AtomicBoolean released = new AtomicBoolean(false);

    public PooledObject(T object) {
        this.object = object;
        createTime = Millisecond100Clock.currentTimeMillis();
        activeTime = createTime;
    }

    void takeFromPool() {
        released.compareAndSet(true, false);
    }

    boolean prepareRelease() {
        return released.compareAndSet(false, true);
    }

    public boolean isReleased() {
        return released.get();
    }

    public T getObject() {
        activeTime = Millisecond100Clock.currentTimeMillis();
        return object;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getActiveTime() {
        return activeTime;
    }
}
