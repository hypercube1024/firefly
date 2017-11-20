package com.firefly.utils.lang.pool;

import com.firefly.utils.function.Action0;
import com.firefly.utils.time.Millisecond100Clock;

import java.lang.ref.PhantomReference;
import java.util.Optional;
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
    private final Action0 leakCallback;
    private volatile PhantomReference<PooledObject<T>> phantomReference;

    public PooledObject(T object, Pool<T> pool, Action0 leakCallback) {
        this.object = object;
        this.pool = pool;
        this.leakCallback = leakCallback;
        createTime = Millisecond100Clock.currentTimeMillis();
        activeTime = createTime;
        phantomReference = pool.getLeakDetector().register(this, leakCallback);
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

    /**
     * Clear leak track
     */
    public void clear() {
        Optional.ofNullable(phantomReference).ifPresent(ref -> pool.getLeakDetector().clear(ref));
    }

    /**
     * Get the leak callback. It is invoked when the pooled object leaks.
     *
     * @return The leak callback.
     */
    public Action0 getLeakCallback() {
        return leakCallback;
    }

    /**
     * Get the phantom reference which is used to track leak.
     *
     * @return The phantom reference which is used to track leak.
     */
    public PhantomReference<PooledObject<T>> getPhantomReference() {
        return phantomReference;
    }

    /**
     * Set the phantom reference which is used to track leak.
     *
     * @param phantomReference The phantom reference which is used to track leak.
     */
    public void setPhantomReference(PhantomReference<PooledObject<T>> phantomReference) {
        this.phantomReference = phantomReference;
    }
}
