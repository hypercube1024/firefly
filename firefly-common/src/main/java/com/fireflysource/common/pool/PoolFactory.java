package com.fireflysource.common.pool;

import com.fireflysource.common.func.Callback;

/**
 * @author Pengtao Qiu
 */
abstract public class PoolFactory {

    public static <T> Pool<T> newPool(int maxSize, long timeout,
                                      Pool.ObjectFactory<T> objectFactory, Pool.Validator<T> validator, Pool.Dispose<T> dispose,
                                      long leakDetectorInterval, long releaseTimeout, Callback noLeakCallback) {
        return new AsyncBoundObjectPool<>(maxSize, timeout,
                objectFactory, validator, dispose,
                leakDetectorInterval, releaseTimeout, noLeakCallback);
    }

}
