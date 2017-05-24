package com.firefly.utils.lang.pool;

import com.firefly.utils.concurrent.Promise;

/**
 * @author Pengtao Qiu
 */
public interface AsynchronousPool<T> extends Pool<T> {

    Promise.Completable<PooledObject<T>> take();

}
