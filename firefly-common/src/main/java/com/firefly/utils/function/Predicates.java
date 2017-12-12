package com.firefly.utils.function;

import java.util.function.Predicate;

/**
 * @author Pengtao Qiu
 */
abstract public class Predicates {
    public static <T> Predicate<T> of(Predicate<T> predicate) {
        return predicate;
    }
}
