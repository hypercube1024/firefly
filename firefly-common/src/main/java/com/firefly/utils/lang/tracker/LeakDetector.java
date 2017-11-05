package com.firefly.utils.lang.tracker;

import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.function.Action0;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
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

    public LeakDetector() {
        this(0L, 15L);
    }

    public LeakDetector(long initialDelay, long delay) {
        this(initialDelay, delay, TimeUnit.SECONDS);
    }

    public LeakDetector(long initialDelay, long delay, TimeUnit unit) {
        this(Schedulers.computation(), initialDelay, delay, unit);
    }

    public LeakDetector(Scheduler scheduler, long initialDelay, long delay, TimeUnit unit) {
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.delay = delay;
        this.unit = unit;
        start();
    }

    /**
     * Create a LeakDetectorReference. When the tracked object is released, you must call the release method of LeakDetectorReference.
     *
     * @param object   The tracked object
     * @param callback When the garbage collector cleans up the tracked object, if the tracked object is not released,
     *                 that means the tracked object leaked. The detector will execute the callback method.
     * @return a new LeakDetectorReference
     */
    public LeakDetectorReference<T> create(T object, Action0 callback) {
        return new LeakDetectorReference<>(object, referenceQueue, callback);
    }

    @Override
    protected void init() {
        scheduler.scheduleAtFixedRate(() -> {
            Reference<? extends T> ref;
            while ((ref = referenceQueue.poll()) != null) {
                try {
                    if (ref instanceof LeakDetectorReference) {
                        ((LeakDetectorReference) ref).getCallback().call();
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }, initialDelay, delay, unit);
    }

    @Override
    protected void destroy() {
        scheduler.stop();
    }
}
