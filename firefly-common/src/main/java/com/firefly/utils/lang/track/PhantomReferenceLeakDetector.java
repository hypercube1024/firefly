package com.firefly.utils.lang.track;

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
 * It helps you to track the resource leak.
 *
 * @author Pengtao Qiu
 */
public class PhantomReferenceLeakDetector<T> extends AbstractLifeCycle {

    private final Scheduler scheduler;
    private final long initialDelay;
    private final long delay;
    private final TimeUnit unit;
    private final ReferenceQueue<T> referenceQueue = new ReferenceQueue<>();
    private final Action0 noLeakCallback;
    private final Map<PhantomReference<T>, Action0> registeredMap = Collections.synchronizedMap(new IdentityHashMap<>());

    /**
     * Construct a leak detector. It helps you to track the resource leak.
     */
    public PhantomReferenceLeakDetector() {
        this(() -> {
        });
    }

    /**
     * Construct a leak detector. It helps you to track the resource leak.
     *
     * @param noLeakCallback If not any resources leak, the detector will execute this action.
     */
    public PhantomReferenceLeakDetector(Action0 noLeakCallback) {
        this(0L, 15L, noLeakCallback);
    }

    /**
     * Construct a leak detector. It helps you to track the resource leak.
     *
     * @param initialDelay   The time to delay first execution. The time unit is second.
     * @param delay          The delay between the termination of one
     *                       execution and the commencement of the next The time unit is second.
     * @param noLeakCallback If not any resources leak, the detector will execute this action.
     */
    public PhantomReferenceLeakDetector(long initialDelay, long delay, Action0 noLeakCallback) {
        this(initialDelay, delay, TimeUnit.SECONDS, noLeakCallback);
    }

    /**
     * Construct a leak detector. It helps you to track the resource leak.
     *
     * @param initialDelay   The time to delay first execution
     * @param delay          The delay between the termination of one
     *                       execution and the commencement of the next
     * @param unit           The time unit of the initialDelay and delay parameters
     * @param noLeakCallback If not any resources leak, the detector will execute this action.
     */
    public PhantomReferenceLeakDetector(long initialDelay, long delay, TimeUnit unit, Action0 noLeakCallback) {
        this(Schedulers.computation(), initialDelay, delay, unit, noLeakCallback);
    }

    /**
     * Construct a leak detector. It helps you to track the resource leak.
     *
     * @param scheduler      The scheduler executes the task that checks the resource leak.
     * @param initialDelay   The time to delay first execution
     * @param delay          The delay between the termination of one
     *                       execution and the commencement of the next
     * @param unit           The time unit of the initialDelay and delay parameters
     * @param noLeakCallback If not any resources leak, the detector will execute this action.
     */
    public PhantomReferenceLeakDetector(Scheduler scheduler, long initialDelay, long delay, TimeUnit unit, Action0 noLeakCallback) {
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.delay = delay;
        this.unit = unit;
        this.noLeakCallback = noLeakCallback;
        start();
    }

    /**
     * Register a tracked object. When the tracked object is released, you must call the clear method of the LeakDetector.
     *
     * @param object       The tracked object
     * @param leakCallback When the garbage collector cleans up the tracked object, if the tracked object is not released,
     *                     that means the tracked object has leaked. The detector will execute the action.
     * @return a new PhantomReference that tracks the object
     */
    public PhantomReference<T> register(T object, Action0 leakCallback) {
        PhantomReference<T> ref = new PhantomReference<>(object, referenceQueue);
        registeredMap.put(ref, leakCallback);
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

    /**
     * The time to delay first execution
     *
     * @return The time to delay first execution
     */
    public long getInitialDelay() {
        return initialDelay;
    }

    /**
     * the delay between the termination of one
     * execution and the commencement of the next
     *
     * @return the delay between the termination of one
     * execution and the commencement of the next
     */
    public long getDelay() {
        return delay;
    }

    /**
     * The time unit of the initialDelay and delay parameters
     *
     * @return The time unit of the initialDelay and delay parameters
     */
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
