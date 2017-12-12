package com.firefly.utils.retry;

import java.util.function.Predicate;

/**
 * @author Pengtao Qiu
 */
abstract public class RetryStrategies {

    /**
     * If the task throws some exceptions, the task will be retried.
     *
     * @param predicate The exception predicate.
     * @param <V>       The return value type.
     * @return The retry strategy predicate.
     */
    public static <V> Predicate<TaskContext<V>> ifException(Predicate<? super Exception> predicate) {
        return ctx -> predicate.test(ctx.getException());
    }

    /**
     * If the task result satisfies condition, the task will be retried.
     *
     * @param predicate The result predicate.
     * @param <V>       The return value type.
     * @return The retry strategy predicate.
     */
    public static <V> Predicate<TaskContext<V>> ifResult(Predicate<V> predicate) {
        return ctx -> predicate.test(ctx.getResult());
    }
}
