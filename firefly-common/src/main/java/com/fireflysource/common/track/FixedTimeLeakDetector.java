package com.fireflysource.common.track;

import com.fireflysource.common.func.Callback;
import com.fireflysource.common.lifecycle.AbstractLifeCycle;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.fireflysource.common.concurrent.ExecutorServiceUtils.shutdownAndAwaitTermination;

/**
 * @author Pengtao Qiu
 */
public class FixedTimeLeakDetector<T> extends AbstractLifeCycle {

    private final ScheduledExecutorService scheduler;
    private final long initialDelay;
    private final long delay;
    private final long releaseTimeout;
    private final TimeUnit unit;
    private final Callback noLeakCallback;
    private final Map<T, TrackedObject> registeredMap = Collections.synchronizedMap(new IdentityHashMap<>());

    public FixedTimeLeakDetector(long interval, long releaseTimeout, Callback noLeakCallback) {
        this(Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "firefly-fixed-time-leak-detector-thread")),
                interval, interval, releaseTimeout, TimeUnit.SECONDS, noLeakCallback);
    }

    public FixedTimeLeakDetector(ScheduledExecutorService scheduler, long initialDelay, long delay, long releaseTimeout, TimeUnit unit,
                                 Callback noLeakCallback) {
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.delay = delay;
        this.unit = unit;
        this.releaseTimeout = releaseTimeout;
        this.noLeakCallback = noLeakCallback;
        start();
    }

    private class TrackedObject {
        long registeredTime;
        Consumer<T> leakCallback;
    }

    public void register(T object, Consumer<T> leakCallback) {
        TrackedObject trackedObject = new TrackedObject();
        trackedObject.leakCallback = leakCallback;
        trackedObject.registeredTime = System.currentTimeMillis();
        registeredMap.put(object, trackedObject);
    }

    public void clear(T object) {
        registeredMap.remove(object);
    }

    @Override
    protected void init() {
        scheduler.scheduleWithFixedDelay(() -> {
            boolean leaked = false;
            for (Map.Entry<T, TrackedObject> e : registeredMap.entrySet()) {
                T obj = e.getKey();
                TrackedObject trackedObject = e.getValue();
                long releaseTimeoutMillis = unit.toMillis(releaseTimeout);
                long currentTime = System.currentTimeMillis();
                if ((currentTime - trackedObject.registeredTime) >= releaseTimeoutMillis) {
                    leaked = true;
                    trackedObject.leakCallback.accept(obj);
                }
            }
            if (!leaked) {
                noLeakCallback.call();
            }
        }, initialDelay, delay, unit);
    }

    @Override
    protected void destroy() {
        shutdownAndAwaitTermination(scheduler, 10, TimeUnit.SECONDS);
    }
}
