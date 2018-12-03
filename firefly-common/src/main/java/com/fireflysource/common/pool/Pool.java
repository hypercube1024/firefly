package com.fireflysource.common.pool;


import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.common.track.FixedTimeLeakDetector;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a cached pool of objects.
 */
public interface Pool<T> extends LifeCycle {

    /**
     * Get the object asynchronously.
     *
     * @return The pooled object.
     */
    CompletableFuture<PooledObject<T>> get();

    /**
     * Returns an instance from the pool. The call may be a blocking one or a
     * non-blocking one and that is determined by the internal implementation.
     * <p>
     * If the call is a blocking call, the call returns immediately with a valid
     * object if available, else the thread is made to wait until an object
     * becomes available. In case of a blocking call, it is advised that clients
     * react to {@link InterruptedException} which might be thrown when the
     * thread waits for an object to become available.
     * <p>
     * If the call is a non-blocking one, the call returns immediately
     * irrespective of whether an object is available or not. If any object is
     * available the call returns it else the call returns null.
     * <p>
     * The validity of the objects are determined using the Validator interface,
     * such that an object o is valid if Validator.isValid(o) == true
     *
     * @return T The pooled object.
     */
    PooledObject<T> take() throws InterruptedException;

    /**
     * Releases the object and puts it back to the pool.
     * <p>
     * The mechanism of putting the object back to the pool is generally
     * asynchronous.
     *
     * @param pooledObject the object to return to the pool
     */
    void release(PooledObject<T> pooledObject);

    /**
     * Check the pooled object. If return true, the object is valid
     *
     * @param pooledObject The pooled object
     * @return if return true, the object is valid.
     */
    boolean isValid(PooledObject<T> pooledObject);

    /**
     * Get the current pool size
     *
     * @return Current pool size
     */
    int size();

    /**
     * If return true, the pool is empty
     *
     * @return If return true, the pool is empty
     */
    boolean isEmpty();

    /**
     * Get the leak detector
     *
     * @return the leak detector
     */
    FixedTimeLeakDetector<PooledObject<T>> getLeakDetector();

    /**
     * Get the created object count.
     *
     * @return The created object count
     */
    int getCreatedObjectCount();

    /**
     * Represents the functionality to validate an object of the pool
     *
     * @param <T> The pooled object
     */
    @FunctionalInterface
    interface Validator<T> {

        /**
         * Checks whether the object is valid.
         *
         * @param pooledObject the object to check.
         * @return true if the object is valid else false.
         */
        boolean isValid(PooledObject<T> pooledObject);
    }

    /**
     * Cleanup the pooled object
     *
     * @param <T> The pooled object
     */
    @FunctionalInterface
    interface Dispose<T> {

        /**
         * Performs any cleanup activities before discarding the object. For
         * example before discarding database connection objects, the pool will
         * want to close the connections.
         *
         * @param pooledObject the object to cleanup
         */
        void destroy(PooledObject<T> pooledObject);
    }

    /**
     * A factory to create a new object
     *
     * @param <T> The pooled object
     */
    @FunctionalInterface
    interface ObjectFactory<T> {

        /**
         * Create a new object in future.
         *
         * @return a future that is a new instance of an object of type T.
         */
        CompletableFuture<PooledObject<T>> createNew(Pool<T> pool);
    }
}
