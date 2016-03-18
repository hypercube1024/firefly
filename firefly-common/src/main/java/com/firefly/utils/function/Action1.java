package com.firefly.utils.function;

/**
 * A one-argument action.
 */
public interface Action1<T> extends Action {
    void call(T t);
}
