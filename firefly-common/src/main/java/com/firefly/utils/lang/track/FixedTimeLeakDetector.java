package com.firefly.utils.lang.track;

import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.time.Millisecond100Clock;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class FixedTimeLeakDetector<T> extends AbstractLifeCycle {

    private final Scheduler scheduler;
    private final long initialDelay;
    private final long delay;
    private final TimeUnit unit;
    private final Action0 noLeakCallback;
    private final Map<T, TrackedObject> registeredMap = Collections.synchronizedMap(new IdentityHashMap<>());

    public FixedTimeLeakDetector(long interval, Action0 noLeakCallback) {
        this(Schedulers.createScheduler(), interval, interval, TimeUnit.SECONDS, noLeakCallback);
    }

    public FixedTimeLeakDetector(Scheduler scheduler, long initialDelay, long delay, TimeUnit unit,
                                 Action0 noLeakCallback) {
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.delay = delay;
        this.unit = unit;
        this.noLeakCallback = noLeakCallback;
        start();
    }

    private class TrackedObject {
        long registeredTime;
        Action1<T> leakCallback;
    }

    public void register(T object, Action1<T> leakCallback) {
        TrackedObject trackedObject = new TrackedObject();
        trackedObject.leakCallback = leakCallback;
        trackedObject.registeredTime = Millisecond100Clock.currentTimeMillis();
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
                long delayMillis = unit.toMillis(delay);
                long currentTime = Millisecond100Clock.currentTimeMillis();
                if ((currentTime - trackedObject.registeredTime) >= delayMillis) {
                    leaked = true;
                    trackedObject.leakCallback.call(obj);
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
