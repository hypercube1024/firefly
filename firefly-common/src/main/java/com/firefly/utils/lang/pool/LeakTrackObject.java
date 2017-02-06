package com.firefly.utils.lang.pool;

/**
 * @author Pengtao Qiu
 */
public class LeakTrackObject<T> {
    private T object;

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }
}
