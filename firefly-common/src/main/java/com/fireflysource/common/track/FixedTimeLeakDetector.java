package com.fireflysource.common.track;

import com.fireflysource.common.func.Callback;
import com.fireflysource.common.lifecycle.AbstractLifeCycle;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

    public FixedTimeLeakDetector(
            ScheduledExecutorService scheduler,
            long initialDelay, long delay, long releaseTimeout, TimeUnit unit,
            Callback noLeakCallback) {
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.delay = delay;
        this.unit = unit;
        this.releaseTimeout = releaseTimeout;
        this.noLeakCallback = noLeakCallback;
        start();
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

    private void checkLeak() {
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

        if (!isStopped()) {
            scheduler.schedule(this::checkLeak, delay, unit);
        }
    }

    @Override
    protected void init() {
        scheduler.schedule(this::checkLeak, initialDelay, unit);
    }

    @Override
    protected void destroy() {
    }

    private class TrackedObject {
        long registeredTime;
        Consumer<T> leakCallback;
    }
}
