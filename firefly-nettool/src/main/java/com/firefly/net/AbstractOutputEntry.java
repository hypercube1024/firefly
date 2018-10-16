package com.firefly.net;

import com.firefly.utils.concurrent.Callback;

public abstract class AbstractOutputEntry<T> implements OutputEntry<T> {

    protected final Callback callback;
    protected final T data;

    public AbstractOutputEntry(Callback callback, T data) {
        this.callback = callback;
        this.data = data;
    }

    @Override
    public Callback getCallback() {
        return callback;
    }

    @Override
    public T getData() {
        return data;
    }

}
