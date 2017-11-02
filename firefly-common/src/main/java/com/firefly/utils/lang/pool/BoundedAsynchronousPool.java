package com.firefly.utils.lang.pool;

import com.firefly.utils.concurrent.Atomics;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.concurrent.ReentrantLocker;
import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pengtao Qiu
 */
public class BoundedAsynchronousPool<T> extends AbstractLifeCycle implements AsynchronousPool<T> {

    protected final int maxSize;
    protected final AtomicInteger createdObjectSize = new AtomicInteger(0);
    protected final long timeout;
    protected final BlockingQueue<PooledObject<T>> queue;
    protected final ExecutorService service;
    protected final ObjectFactory<T> objectFactory;
    protected final Validator<T> validator;
    protected final Dispose<T> dispose;
    protected final ReentrantLocker locker = new ReentrantLocker();

    public BoundedAsynchronousPool(ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(32, objectFactory, validator, dispose);
    }

    public BoundedAsynchronousPool(int maxSize, ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(maxSize, 5000L, objectFactory, validator, dispose);
    }

    public BoundedAsynchronousPool(int maxSize, long timeout,
                                   ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(maxSize, timeout,
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> new Thread(r, "firefly bounded asynchronous pool")),
                objectFactory, validator, dispose);
    }

    public BoundedAsynchronousPool(int maxSize, long timeout,
                                   ExecutorService service,
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

    protected void createObject(Promise.Completable<PooledObject<T>> completable) {
        try {
            Atomics.getAndIncrement(createdObjectSize, maxSize);
            CompletableFuture<PooledObject<T>> tmp = objectFactory.createNew();
            tmp.thenAccept(completable::succeeded)
               .exceptionally(e0 -> {
                   Atomics.getAndDecrement(createdObjectSize, 0);
                   completable.failed(e0);
                   return null;
               });
        } catch (Exception e) {
            System.err.println(e.getMessage());
            Atomics.getAndDecrement(createdObjectSize, 0);
        }
    }

    protected void destroyObject(PooledObject<T> t) {
        Atomics.getAndDecrement(createdObjectSize, 0);
        try {
            dispose.destroy(t);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<PooledObject<T>> take() {
        Promise.Completable<PooledObject<T>> completable = new Promise.Completable<>();
        PooledObject<T> pooledObject = queue.poll();
        if (pooledObject != null) {
            checkObjectFromPool(pooledObject, completable);
            return completable;
        } else { // the queue is empty
            return locker.lock(() -> {
                int availableSize = maxSize - getCreatedObjectSize();
                if (availableSize > 0) { // the pool is not full
                    createObject(completable);
                    return completable;
                } else {
                    service.execute(() -> {
                        try {
                            PooledObject<T> object = queue.poll(timeout, TimeUnit.MILLISECONDS);
                            if (object != null) {
                                checkObjectFromPool(object, completable);
                            } else {
                                completable.failed(new TimeoutException("take pooled object timeout"));
                            }
                        } catch (InterruptedException e) {
                            completable.failed(e);
                        }
                    });
                    return completable;
                }
            });
        }
    }

    private void checkObjectFromPool(PooledObject<T> pooledObject, Promise.Completable<PooledObject<T>> completable) {
        if (pooledObject.prepareTake()) {
            if (validator.isValid(pooledObject)) {
                completable.succeeded(pooledObject);
            } else {
                destroyObject(pooledObject);
                createObject(completable);
            }
        } else {
            completable.failed(new CommonRuntimeException("the pooled object has been used"));
        }
    }

    @Override
    public void release(PooledObject<T> t) {
        if (t == null) {
            return;
        }
        if (!t.prepareRelease()) { // Object is released
            return;
        }
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

    @Override
    public PooledObject<T> get() {
        throw new CommonRuntimeException("the method is not implemented");
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
        try {
            PooledObject<T> t;
            while ((t = queue.poll()) != null) {
                t.prepareTake();
                destroyObject(t);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        if (service != null) {
            service.shutdown();
        }
    }
}
