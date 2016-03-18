package com.firefly.utils.function;

/**
 * A vector-argument action.
 */
public interface ActionN extends Action {
    void call(Object... args);
}
