package com.firefly.utils.lang.pool;

import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.lang.ref.SoftReference;

/**
 * @author Pengtao Qiu
 */
public class SoftReferenceAsynchronousPool<T> extends AbstractLifeCycle implements AsynchronousPool<T> {

    @SuppressWarnings("unchecked")
    private SoftReference<T>[] pool = new SoftReference[8];
    private CompletableObjectFactory<T> objectFactory;
    private Validator<T> validator;
    private Dispose<T> dispose;

    public SoftReferenceAsynchronousPool(CompletableObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this.objectFactory = objectFactory;
        this.validator = validator;
        this.dispose = dispose;
    }

    @Override
    public Promise.Completable<T> take() {
        T t = get();
        if (t == null) {
            return objectFactory.createNew();
        } else {
            Promise.Completable<T> completable = new Promise.Completable<>();
            completable.succeeded(t);
            return completable;
        }
    }

    @Override
    public T get() {
        for (int i = 0; i < pool.length; i++) {
            SoftReference<T> ref = pool[i];
            if (ref != null && ref.get() != null && validator.isValid(ref.get())) {
                pool[i] = null;
                return ref.get();
            }
        }
        return null;
    }

    @Override
    public void release(T t) {
        for (int i = 0; i < pool.length; i++) {
            SoftReference<T> ref = pool[i];
            if (ref == null || ref.get() == null || !validator.isValid(ref.get())) {
                pool[i] = new SoftReference<>(t);
                return;
            }
        }

        // the pool is full
        dispose.destroy(t);
    }

    @Override
    public int size() {
        int count = 0;
        for (int i = 0; i < pool.length; i++) {
            SoftReference<T> ref = pool[i];
            if (ref != null && ref.get() != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < pool.length; i++) {
            SoftReference<T> ref = pool[i];
            if (ref != null && ref.get() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void init() {
    }

    @Override
    protected void destroy() {
        for (int i = 0; i < pool.length; i++) {
            SoftReference<T> ref = pool[i];
            if (ref != null) {
                T t = ref.get();
                if (t != null) {
                    dispose.destroy(t);
                    pool[i] = null;
                }
            }
        }
    }
}
