package com.firefly.utils.lang;

import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.function.Action0;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class LeakDetector<T> extends AbstractLifeCycle {

    private final Scheduler scheduler;
    private final long initialDelay;
    private final long delay;
    private final TimeUnit unit;
    private final ReferenceQueue<T> referenceQueue = new ReferenceQueue<>();
    private final Action0 noLeakCallback;
    private final Map<PhantomReference<T>, Action0> registeredMap = Collections.synchronizedMap(new IdentityHashMap<>());

    public LeakDetector() {
        this(() -> {
        });
    }

    public LeakDetector(Action0 noLeakCallback) {
        this(0L, 15L, noLeakCallback);
    }

    public LeakDetector(long initialDelay, long delay, Action0 noLeakCallback) {
        this(initialDelay, delay, TimeUnit.SECONDS, noLeakCallback);
    }

    public LeakDetector(long initialDelay, long delay, TimeUnit unit, Action0 noLeakCallback) {
        this(Schedulers.computation(), initialDelay, delay, unit, noLeakCallback);
    }

    public LeakDetector(Scheduler scheduler, long initialDelay, long delay, TimeUnit unit, Action0 noLeakCallback) {
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.delay = delay;
        this.unit = unit;
        this.noLeakCallback = noLeakCallback;
        start();
    }

    /**
     * Register a tracked object. When the tracked object is released, you must call the clear method of LeakDetector.
     *
     * @param object   The tracked object
     * @param callback When the garbage collector cleans up the tracked object, if the tracked object is not released,
     *                 that means the tracked object leaked. The detector will execute the callback method.
     * @return a new PhantomReference that tracks the object
     */
    public PhantomReference<T> register(T object, Action0 callback) {
        PhantomReference<T> ref = new PhantomReference<>(object, referenceQueue);
        registeredMap.put(ref, callback);
        return ref;
    }

    /**
     * Clear tracked object
     *
     * @param reference The PhantomReference of tracked object
     */
    public void clear(PhantomReference<T> reference) {
        Optional.ofNullable(registeredMap.remove(reference)).ifPresent(a -> reference.clear());
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public long getDelay() {
        return delay;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    @Override
    protected void init() {
        scheduler.scheduleAtFixedRate(() -> {
            boolean leaked = false;
            Reference<? extends T> ref;
            while ((ref = referenceQueue.poll()) != null) {
                try {
                    leaked = true;
                    Optional.ofNullable(registeredMap.remove(ref)).ifPresent(Action0::call);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
            if (!leaked) {
                noLeakCallback.call();
            }
        }, initialDelay, delay, unit);
    }

    @Override
    protected void destroy() {
        scheduler.stop();
    }
}
