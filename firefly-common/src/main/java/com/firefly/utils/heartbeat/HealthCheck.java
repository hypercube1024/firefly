package com.firefly.utils.heartbeat;

import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * The health check helps you to check the object is alive.
 *
 * @author Pengtao Qiu
 */
public class HealthCheck extends AbstractLifeCycle {

    private final ConcurrentMap<String, Task> checkTaskMap = new ConcurrentHashMap<>();
    private final Scheduler scheduler;
    private final long initialDelay;
    private final long delay;
    private final TimeUnit unit;

    /**
     * Construct a HealthCheck. It helps you to check the object is alive.
     */
    public HealthCheck() {
        this(0L, 10L, TimeUnit.SECONDS);
    }

    /**
     * Construct a HealthCheck. It helps you to check the object is alive.
     *
     * @param initialDelay The time to delay first execution
     * @param delay        The delay between the termination of one
     *                     execution and the commencement of the next
     * @param unit         The time unit of the initialDelay and delay parameters
     */
    public HealthCheck(long initialDelay, long delay, TimeUnit unit) {
        this(Schedulers.createScheduler(), initialDelay, delay, unit);
    }

    /**
     * Construct a HealthCheck. It helps you to check the object is alive.
     *
     * @param scheduler    The scheduler executes the task that checks the object is alive.
     * @param initialDelay The time to delay first execution
     * @param delay        The delay between the termination of one
     *                     execution and the commencement of the next
     * @param unit         The time unit of the initialDelay and delay parameters
     */
    public HealthCheck(Scheduler scheduler, long initialDelay, long delay, TimeUnit unit) {
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.delay = delay;
        this.unit = unit;
    }

    /**
     * Register a health check task.
     *
     * @param task The task function.
     */
    public void register(Task task) {
        Optional.ofNullable(task)
                .filter(t -> t.getTask() != null)
                .filter(t -> StringUtils.hasText(t.getName()))
                .ifPresent(t -> checkTaskMap.put(t.getName(), t));
    }

    /**
     * Clear the health check task.
     *
     * @param name The task name.
     */
    public void clear(String name) {
        checkTaskMap.remove(name);
    }

    @Override
    protected void init() {
        scheduler.scheduleAtFixedRate(() -> checkTaskMap.forEach((name, task) -> {
            try {
                task.getTask().call()
                    .thenAccept(result -> Optional.ofNullable(task.getResultListener()).ifPresent(ret -> ret.call(name, result, null)))
                    .exceptionally(ex -> {
                        Optional.ofNullable(task.getResultListener()).ifPresent(ret -> ret.call(name, Result.FAILURE, ex));
                        return null;
                    });
            } catch (Exception ex) {
                Optional.ofNullable(task.getResultListener()).ifPresent(ret -> ret.call(name, Result.FAILURE, ex));
            }
        }), initialDelay, delay, unit);
    }

    @Override
    protected void destroy() {
        checkTaskMap.clear();
        scheduler.stop();
    }
}
