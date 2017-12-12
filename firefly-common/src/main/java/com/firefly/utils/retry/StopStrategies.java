package com.firefly.utils.retry;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author Pengtao Qiu
 */
abstract public class StopStrategies {

    public static <V> Predicate<TaskContext<V>> afterDelay(long time, TimeUnit timeUnit) {
        return ctx -> (System.currentTimeMillis() - ctx.getStartTime()) >= timeUnit.toMillis(time);
    }

    public static <V> Predicate<TaskContext<V>> afterExecute(int executedCount) {
        return ctx -> ctx.getExecutedCount() >= executedCount;
    }

    public static <V> Predicate<TaskContext<V>> never() {
        return ctx -> false;
    }
}
