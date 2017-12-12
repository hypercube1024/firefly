package com.firefly.utils.retry;

import java.util.function.Predicate;

/**
 * @author Pengtao Qiu
 */
abstract public class StopStrategies {
    public static <V> Predicate<TaskContext<V>> afterAttempt(int retryCount) {
        return ctx -> ctx.getExecutedCount() > retryCount;
    }

    public static <V> Predicate<TaskContext<V>> never() {
        return ctx -> false;
    }
}
