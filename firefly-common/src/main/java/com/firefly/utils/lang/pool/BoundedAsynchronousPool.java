package com.firefly.utils.lang.pool;

import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pengtao Qiu
 */
public class BoundedAsynchronousPool<T> extends AbstractLifeCycle implements AsynchronousPool<T> {

    private int maxSize;
    private AtomicInteger createdObjectSize = new AtomicInteger(0);
    private long timeout = 5000L;
    private BlockingQueue<PooledObject<T>> queue;
    private ExecutorService service;
    private ObjectFactory<T> objectFactory;
    private Validator<T> validator;
    private Dispose<T> dispose;

    public BoundedAsynchronousPool(ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(32, objectFactory, validator, dispose);
    }

    public BoundedAsynchronousPool(int maxSize, ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(maxSize, 5000L, objectFactory, validator, dispose);
    }

    public BoundedAsynchronousPool(int maxSize, long timeout,
                                   ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(maxSize, timeout,
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
                objectFactory, validator, dispose);
    }

    public BoundedAsynchronousPool(int maxSize, long timeout, ExecutorService service,
                                   ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this.maxSize = maxSize;
        this.timeout = timeout;
        this.service = service;
        this.objectFactory = objectFactory;
        this.validator = validator;
        this.dispose = dispose;
        this.queue = new ArrayBlockingQueue<>(maxSize);
        start();
    }

    private Promise.Completable<PooledObject<T>> createObject() {
        Promise.Completable<PooledObject<T>> r = new Promise.Completable<>();
        createObject(r);
        return r;
    }

    private void createObject(Promise.Completable<PooledObject<T>> completable) {
        createdObjectSize.incrementAndGet();
        Promise.Completable<PooledObject<T>> tmp = objectFactory.createNew();
        tmp.thenAccept(completable::succeeded)
           .exceptionally(e0 -> {
               createdObjectSize.decrementAndGet();
               completable.failed(e0);
               return null;
           });
    }

    private void destroyObject(PooledObject<T> t) {
        createdObjectSize.decrementAndGet();
        dispose.destroy(t);
    }

    @Override
    public Promise.Completable<PooledObject<T>> take() {
        PooledObject<T> t = get();
        if (t != null) {
            if (validator.isValid(t)) {
                Promise.Completable<PooledObject<T>> completable = new Promise.Completable<>();
                completable.succeeded(t);
                return completable;
            } else {
                destroyObject(t);
                return createObject();
            }
        } else {
            // the queue is empty
            if (maxSize - getCreatedObjectSize() > 0) {
                return createObject();
            } else {
                Promise.Completable<PooledObject<T>> completable = new Promise.Completable<>();
                service.execute(() -> {
                    try {
                        PooledObject<T> r = queue.poll(timeout, TimeUnit.MILLISECONDS);
                        if (r != null) {
                            r.takeFromPool();
                            if (validator.isValid(r)) {
                                completable.succeeded(r);
                            } else {
                                destroyObject(r);
                                createObject(completable);
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
    public void release(PooledObject<T> t) {
        if (t != null) {
            if (validator.isValid(t)) {
                if (t.prepareRelease()) {
                    boolean success = queue.offer(t);
                    if (!success) {
                        // the queue is full
                        service.execute(() -> {
                            try {
                                boolean success0 = queue.offer(t, timeout, TimeUnit.MILLISECONDS);
                                if (!success0) {
                                    destroyObject(t);
                                }
                            } catch (InterruptedException e) {
                                destroyObject(t);
                            }
                        });
                    }
                }
            } else {
                destroyObject(t);
            }
        }
    }

    @Override
    public PooledObject<T> get() {
        PooledObject<T> t = queue.poll();
        if (t != null) {
            t.takeFromPool();
            return t;
        } else {
            return t;
        }
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public int getCreatedObjectSize() {
        return createdObjectSize.get();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean isValid(PooledObject<T> t) {
        return validator.isValid(t);
    }

    @Override
    protected void init() {
    }

    @Override
    protected void destroy() {
        PooledObject<T> t;
        while ((t = queue.poll()) != null) {
            t.takeFromPool();
            destroyObject(t);
        }
        if (service != null) {
            service.shutdown();
        }
    }
}
