package com.firefly.utils.retry;

import java.util.function.Predicate;

/**
 * @author Pengtao Qiu
 */
abstract public class RetryStrategies {

    public static <V> Predicate<TaskContext<V>> ifException(Predicate<? super Exception> predicate) {
        return ctx -> predicate.test(ctx.getException());
    }

    public static <V> Predicate<TaskContext<V>> ifResult(Predicate<V> predicate) {
        return ctx -> predicate.test(ctx.getResult());
    }
}
