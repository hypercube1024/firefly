package com.firefly.utils.retry;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author Pengtao Qiu
 */
abstract public class StopStrategies {

    /**
     * If the task executed time exceeds the specified time, the task will stop retry.
     *
     * @param time     The max task executed time.
     * @param timeUnit The time unit.
     * @param <V>      The return value type.
     * @return The stop strategy predicate.
     */
    public static <V> Predicate<TaskContext<V>> afterDelay(long time, TimeUnit timeUnit) {
        return ctx -> (System.currentTimeMillis() - ctx.getStartTime()) >= timeUnit.toMillis(time);
    }

    /**
     * If the task executed count exceeds the max count, the task will stop retry.
     *
     * @param count The task max executed count.
     * @param <V>   The return value type.
     * @return The stop strategy predicate.
     */
    public static <V> Predicate<TaskContext<V>> afterExecute(int count) {
        return ctx -> ctx.getExecutedCount() >= count;
    }

    /**
     * The task never stop.
     *
     * @param <V> The return value type.
     * @return The stop strategy predicate.
     */
    public static <V> Predicate<TaskContext<V>> never() {
        return ctx -> false;
    }
}
