package com.fireflysource.common.pool;

public class FakePooledObject<T> extends PooledObject<T> {

    public FakePooledObject(T object) {
        super(object, null, null);
    }

    @Override
    public void release() {

    }

    @Override
    public void close() {
    }
}
