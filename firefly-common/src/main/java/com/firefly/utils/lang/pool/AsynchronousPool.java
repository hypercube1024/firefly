package com.firefly.utils.lang.pool;

import java.util.concurrent.CompletableFuture;

/**
 * The cache of objects. It can take object asynchronously
 *
 * @author Pengtao Qiu
 */
public interface AsynchronousPool<T> extends Pool<T> {

    /**
     * Take the object asynchronously
     *
     * @return The asynchronous result of the pooled object
     */
    CompletableFuture<PooledObject<T>> take();

}
