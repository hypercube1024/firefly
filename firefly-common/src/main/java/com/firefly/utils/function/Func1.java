package com.firefly.utils.function;

/**
 * Represents a function with one argument.
 */
public interface Func1<T, R> extends Function {
    R call(T t);
}
