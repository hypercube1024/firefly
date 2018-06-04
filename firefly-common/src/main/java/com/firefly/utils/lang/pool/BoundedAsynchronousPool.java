package com.firefly.utils.lang.pool;

import com.firefly.utils.concurrent.Atomics;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.concurrent.ReentrantLocker;
import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.function.Action0;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.lang.LeakDetector;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pengtao Qiu
 */
public class BoundedAsynchronousPool<T> extends AbstractLifeCycle implements AsynchronousPool<T> {

    public static final int defaultPoolServiceThreadNumber = Integer.getInteger("com.firefly.utils.lang.pool.asynchronous.number", Runtime.getRuntime().availableProcessors());

    protected final int maxSize;
    protected final AtomicInteger createdObjectSize = new AtomicInteger(0);
    protected final long timeout;
    protected final BlockingQueue<PooledObject<T>> queue;
    protected final ExecutorService service;
    protected final ObjectFactory<T> objectFactory;
    protected final Validator<T> validator;
    protected final Dispose<T> dispose;
    protected final LeakDetector<PooledObject<T>> leakDetector;
    protected final ReentrantLocker locker = new ReentrantLocker();

    public BoundedAsynchronousPool(ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(32, objectFactory, validator, dispose);
    }

    public BoundedAsynchronousPool(int maxSize, ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose) {
        this(maxSize, 5000L, objectFactory, validator, dispose, () -> {
        });
    }

    public BoundedAsynchronousPool(int maxSize, long timeout,
                                   ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose,
                                   Action0 noLeakCallback) {
        this(maxSize, timeout,
                Executors.newFixedThreadPool(defaultPoolServiceThreadNumber, r -> new Thread(r, "firefly bounded asynchronous pool")),
                objectFactory, validator, dispose,
                new LeakDetector<>(noLeakCallback));
    }

    public BoundedAsynchronousPool(int maxSize, long timeout,
                                   ExecutorService service,
                                   ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose,
                                   LeakDetector<PooledObject<T>> leakDetector) {
        this.maxSize = maxSize;
        this.timeout = timeout;
        this.service = service;
        this.objectFactory = objectFactory;
        this.validator = validator;
        this.dispose = dispose;
        this.queue = new ArrayBlockingQueue<>(maxSize);
        this.leakDetector = leakDetector;
        start();
    }

    protected void createObject(Promise.Completable<PooledObject<T>> completable) {
        try {
            increaseCreatedObjectSize();
            CompletableFuture<PooledObject<T>> tmp = objectFactory.createNew(this);
            tmp.thenAccept(completable::succeeded).exceptionally(e0 -> {
                decreaseCreatedObjectSize();
                completable.failed(e0);
                return null;
            });
        } catch (Exception e) {
            System.err.println(e.getMessage());
            decreaseCreatedObjectSize();
        }
    }

    protected void destroyObject(PooledObject<T> pooledObject) {
        decreaseCreatedObjectSize();
        try {
            dispose.destroy(pooledObject);
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
                if (availableSize > 0) {
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
                pooledObject.setPhantomReference(getLeakDetector().register(pooledObject, pooledObject.getLeakCallback()));
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
    public void release(PooledObject<T> pooledObject) {
        if (pooledObject == null) {
            return;
        }
        if (!pooledObject.prepareRelease()) { // Object is released
            return;
        }

        if (queue.offer(pooledObject)) {
            pooledObject.clear();
        } else {
            // the queue is full
            service.execute(() -> {
                try {
                    if (!queue.offer(pooledObject, timeout, TimeUnit.MILLISECONDS)) {
                        destroyObject(pooledObject);
                    }
                } catch (InterruptedException e) {
                    destroyObject(pooledObject);
                }
            });
        }
    }

    @Override
    public PooledObject<T> get() {
        try {
            return take().get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            return null;
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
    public boolean isValid(PooledObject<T> pooledObject) {
        return validator.isValid(pooledObject);
    }

    @Override
    public LeakDetector<PooledObject<T>> getLeakDetector() {
        return leakDetector;
    }

    @Override
    public void increaseCreatedObjectSize() {
        Atomics.getAndIncrement(createdObjectSize, maxSize);
    }

    @Override
    public void decreaseCreatedObjectSize() {
        Atomics.getAndDecrement(createdObjectSize, 0);
    }

    @Override
    protected void init() {
    }

    @Override
    protected void destroy() {
        try {
            PooledObject<T> pooledObject;
            while ((pooledObject = queue.poll()) != null) {
                pooledObject.prepareTake();
                destroyObject(pooledObject);
            }
            leakDetector.stop();
            service.shutdown();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
