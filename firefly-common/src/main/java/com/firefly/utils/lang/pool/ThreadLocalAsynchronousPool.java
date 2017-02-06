package com.firefly.utils.lang.pool;

import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class ThreadLocalAsynchronousPool<T> extends AbstractLifeCycle implements AsynchronousPool<T> {

    private final ThreadLocal<SoftReferenceAsynchronousPool<T>> pool;

    public ThreadLocalAsynchronousPool(CompletableObjectFactory<T> objectFactory, Pool.Validator<T> validator, Pool.Dispose<T> dispose) {
        pool = ThreadLocal.withInitial(() -> new SoftReferenceAsynchronousPool<>(objectFactory, validator, dispose));
        start();
    }

    @Override
    public Promise.Completable<T> take() {
        return pool.get().take();
    }

    @Override
    public T get() {
        return pool.get().get();
    }

    @Override
    public void release(T t) {
        pool.get().release(t);
    }

    @Override
    public int size() {
        return pool.get().size();
    }

    @Override
    public boolean isEmpty() {
        return pool.get().isEmpty();
    }

    @Override
    protected void init() {
        pool.get().init();
    }

    @Override
    protected void destroy() {
        pool.get().destroy();
    }
}
