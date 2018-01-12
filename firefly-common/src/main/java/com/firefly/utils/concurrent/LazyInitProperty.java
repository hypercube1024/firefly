package com.firefly.utils.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author Pengtao Qiu
 */
public class LazyInitProperty<T> {

    protected AtomicReference<T> reference = new AtomicReference<>();

    public T getProperty(Supplier<T> supplier) {
        T property = reference.get();
        if (property == null) {
            property = supplier.get();
            if (!reference.compareAndSet(null, property)) {
                property = reference.get();
            }
        }
        return property;
    }

}
