package com.firefly.utils.lang.pool;

import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.concurrent.SimpleLock;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class BoundedAsynchronousPool<T> extends AbstractLifeCycle implements AsynchronousPool<T> {

    private int maxSize;
    private int createdObjectSize;
    private long timeout = 2000L;
    private BlockingQueue<T> queue;
    private ExecutorService service;
    private final SimpleLock lock = new SimpleLock();
    private CompletableObjectFactory<T> objectFactory;
    private Validator<T> validator;
    private Dispose<T> dispose;

    public BoundedAsynchronousPool(int maxSize, long timeout, ExecutorService service,
                                   CompletableObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this.maxSize = maxSize;
        this.timeout = timeout;
        this.service = service;
        this.objectFactory = objectFactory;
        this.validator = validator;
        this.dispose = dispose;
    }

    private boolean canCreateObject() {
        return maxSize - createdObjectSize > 0;
    }

    private Promise.Completable<T> createObject() {
        return lock.lock(() -> {
            if (canCreateObject()) {
                createdObjectSize++;
                return objectFactory.createNew();
            } else {
                return null;
            }
        });
    }

    public void decreaseObjectSize() {
        lock.lock(() -> {
            createdObjectSize--;
            return createdObjectSize;
        });
    }

    @Override
    public Promise.Completable<T> take() {
        T t = queue.poll();
        if (t != null) {
            if (validator.isValid(t)) {
                Promise.Completable<T> completable = new Promise.Completable<>();
                completable.succeeded(t);
                return completable;
            } else {
                dispose.destroy(t);
                return objectFactory.createNew();
            }
        } else {
            Promise.Completable<T> ret = createObject();
            if (ret != null) {
                return ret;
            } else {
                Promise.Completable<T> completable = new Promise.Completable<>();
                service.execute(() -> {
                    try {
                        completable.succeeded(queue.poll(timeout, TimeUnit.MILLISECONDS));
                    } catch (InterruptedException e) {
                        completable.failed(e);
                    }
                });
                return completable;
            }
        }
    }

    @Override
    public T get() {
        throw new RuntimeException("not implement");
    }

    @Override
    public void release(T t) {

    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {

    }
}
