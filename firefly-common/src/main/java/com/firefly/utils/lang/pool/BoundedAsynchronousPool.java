package com.firefly.utils.lang.pool;

import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.concurrent.SimpleLock;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.concurrent.*;

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

    public BoundedAsynchronousPool(CompletableObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(32, objectFactory, validator, dispose);
    }

    public BoundedAsynchronousPool(int maxSize, CompletableObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(maxSize, 5000L, objectFactory, validator, dispose);
    }

    public BoundedAsynchronousPool(int maxSize, long timeout,
                                   CompletableObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(maxSize, timeout,
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
                objectFactory, validator, dispose);
    }

    public BoundedAsynchronousPool(int maxSize, long timeout, ExecutorService service,
                                   CompletableObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this.maxSize = maxSize;
        this.timeout = timeout;
        this.service = service;
        this.objectFactory = objectFactory;
        this.validator = validator;
        this.dispose = dispose;
        this.queue = new ArrayBlockingQueue<>(maxSize);
        start();
    }

    private Promise.Completable<T> createObject() {
        return lock.lock(() -> {
            if (maxSize - createdObjectSize > 0) {
                createdObjectSize++;
                Promise.Completable<T> r = new Promise.Completable<>();
                Promise.Completable<T> completable = objectFactory.createNew();
                completable.thenAccept(r::succeeded)
                           .exceptionally(e -> {
                               // create object failure, decrease created object size
                               decreaseObjectSize();
                               r.failed(e);
                               return null;
                           });
                return r;
            } else {
                return null;
            }
        });
    }

    private void decreaseObjectSize() {
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
                // the object is invalid, the created object size is not changed;
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
                        T r = queue.poll(timeout, TimeUnit.MILLISECONDS);
                        if (r != null) {
                            if (validator.isValid(r)) {
                                completable.succeeded(r);
                            } else {
                                // the object is invalid, the created object size is not changed;
                                dispose.destroy(r);
                                Promise.Completable<T> tmp = objectFactory.createNew();
                                tmp.thenAccept(completable::succeeded)
                                   .exceptionally(e0 -> {
                                       completable.failed(e0);
                                       return null;
                                   });
                            }
                        } else {
                            completable.failed(new TimeoutException("take pooled object timeout"));
                        }
                    } catch (InterruptedException e) {
                        completable.failed(e);
                    }
                });
                return completable;
            }
        }
    }

    @Override
    public void release(T t) {
        if (t != null) {
            if (validator.isValid(t)) {
                if (!queue.contains(t)) {
                    boolean success = queue.offer(t);
                    if (!success) {
                        // the queue is full
                        service.execute(() -> {
                            try {
                                queue.offer(t, timeout, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                dispose.destroy(t);
                            }
                        });
                    }
                }
            } else {
                dispose.destroy(t);
                decreaseObjectSize();
            }
        }
    }

    @Override
    public T get() {
        throw new RuntimeException("not implement");
    }


    @Override
    public int size() {
        return queue.size();
    }

    public int getCreatedObjectSize() {
        return createdObjectSize;
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    protected void init() {
    }

    @Override
    protected void destroy() {
        if (service != null) {
            service.shutdown();
        }
        T t;
        while ((t = queue.poll()) != null) {
            dispose.destroy(t);
        }
        lock.lock(() -> {
            createdObjectSize = 0;
            return createdObjectSize;
        });
    }
}
