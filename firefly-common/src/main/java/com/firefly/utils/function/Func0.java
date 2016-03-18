package com.firefly.utils.function;

import java.util.concurrent.Callable;

/**
 * Represents a function with zero arguments.
 */
public interface Func0<R> extends Function, Callable<R> {
    @Override
    R call();
}
