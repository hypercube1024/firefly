package com.firefly.utils.lang.pool;

import com.firefly.utils.function.Action1;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.lang.track.FixedTimeLeakDetector;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pengtao Qiu
 */
public class UnboundPool<T> extends AbstractLifeCycle implements Pool<T> {

    protected final int maxSize;
    protected final long timeout; // unit second
    protected final long leakDetectorInterval; // unit second
    protected final TimeUnit timeUnit = TimeUnit.SECONDS;
    protected final BlockingQueue<PooledObject<T>> queue;
    protected final ExecutorService gettingService;
    protected final ExecutorService releaseService;
    protected final ExecutorService creatingService;
    protected final ObjectFactory<T> objectFactory;
    protected final Validator<T> validator;
    protected final Dispose<T> dispose;
    protected final FixedTimeLeakDetector<PooledObject<T>> leakDetector;
    protected final AtomicBoolean creatingThreadFlag = new AtomicBoolean(true);

    public UnboundPool(int maxSize, long timeout, long leakDetectorInterval,
                       ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose,
                       Action1<PooledObject<T>> noLeakCallback) {
        this(maxSize, timeout, leakDetectorInterval, 4, 4,
                objectFactory, validator, dispose, noLeakCallback);
    }

    public UnboundPool(int maxSize, long timeout, long leakDetectorInterval,
                       int maxGettingThreadNum, int maxReleaseThreadNum,
                       ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose,
                       Action1<PooledObject<T>> noLeakCallback) {
        this(maxSize, timeout, leakDetectorInterval,
                new ArrayBlockingQueue<>(maxSize),
                new ThreadPoolExecutor(1, maxGettingThreadNum,
                        30L, TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(10000),
                        r -> new Thread(r, "firefly-asynchronous-pool-getting-thread")),
                new ThreadPoolExecutor(1, maxReleaseThreadNum,
                        30L, TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(10000),
                        r -> new Thread(r, "firefly-asynchronous-pool-release-thread")),
                objectFactory, validator, dispose, new FixedTimeLeakDetector<>(leakDetectorInterval, noLeakCallback));
    }

    public UnboundPool(int maxSize, long timeout, long leakDetectorInterval,
                       BlockingQueue<PooledObject<T>> queue,
                       ExecutorService gettingService, ExecutorService releaseService,
                       ObjectFactory<T> objectFactory, Validator<T> validator, Dispose<T> dispose,
                       FixedTimeLeakDetector<PooledObject<T>> leakDetector) {
        this.maxSize = maxSize;
        this.timeout = timeout;
        this.leakDetectorInterval = leakDetectorInterval;
        this.queue = queue;
        this.gettingService = gettingService;
        this.releaseService = releaseService;
        this.objectFactory = objectFactory;
        this.validator = validator;
        this.dispose = dispose;
        this.leakDetector = leakDetector;
        this.creatingService = Executors.newSingleThreadExecutor(r -> new Thread(r, "firefly-asynchronous-pool-creating-thread"));
    }

    @Override
    public CompletableFuture<PooledObject<T>> asyncGet() {
        CompletableFuture<PooledObject<T>> future = new CompletableFuture<>();
        gettingService.submit(() -> {
            try {
                PooledObject<T> object = get();
                future.complete(object);
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public PooledObject<T> get() throws InterruptedException {
        return pollAndCheck();
    }

    @Override
    public void release(PooledObject<T> pooledObject) {
        releaseService.submit(() -> {
            try {
                queue.offer(pooledObject, timeout, timeUnit);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                dispose.destroy(pooledObject);
                pooledObject.clear();
            }
        });
    }

    private PooledObject<T> pollAndCheck() throws InterruptedException {
        PooledObject<T> pooledObject;
        while ((pooledObject = queue.poll(timeout, timeUnit)) != null) {
            if (isValid(pooledObject)) {
                pooledObject.register();
                return pooledObject;
            }
        }
        throw new IllegalStateException("Can not get the PooledObject");
    }

    @Override
    public boolean isValid(PooledObject<T> pooledObject) {
        return validator.isValid(pooledObject);
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public FixedTimeLeakDetector<PooledObject<T>> getLeakDetector() {
        return leakDetector;
    }

    @Override
    protected void init() {
        creatingService.submit(() -> {
            while (creatingThreadFlag.get()) {
                PooledObject<T> pooledObject = null;
                try {
                    pooledObject = objectFactory.createNew(this).get(timeout, timeUnit);
                    queue.offer(pooledObject, timeout, timeUnit);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    System.err.println(e.getMessage());
                    if (pooledObject != null) {
                        dispose.destroy(pooledObject);
                    }
                }
            }
        });
    }

    @Override
    protected void destroy() {
        creatingThreadFlag.set(false);
        creatingService.shutdown();
        gettingService.shutdown();
        releaseService.shutdown();
        leakDetector.stop();
    }
}
