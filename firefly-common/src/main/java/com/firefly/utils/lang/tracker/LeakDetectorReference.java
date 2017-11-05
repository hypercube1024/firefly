package com.firefly.utils.lang.tracker;

import com.firefly.utils.function.Action0;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * @author Pengtao Qiu
 */
public class LeakDetectorReference<T> extends PhantomReference<T> {

    private final Action0 callback;

    public LeakDetectorReference(T referent, ReferenceQueue<? super T> q, Action0 callback) {
        super(referent, q);
        this.callback = callback;
    }

    public Action0 getCallback() {
        return callback;
    }
}
