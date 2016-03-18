package com.firefly.utils.function;

/**
 * Represents a vector-argument function.
 */
public interface FuncN<R> extends Function {
    R call(Object... args);
}
