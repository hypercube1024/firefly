package com.firefly.utils.lang.pool;

import com.firefly.utils.lang.LifeCycle;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a cached pool of objects.
 */
public interface Pool<T> extends LifeCycle {

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
     * @return T one of the pooled objects.
     */
    PooledObject<T> get();

    /**
     * Releases the object and puts it back to the pool.
     * <p>
     * The mechanism of putting the object back to the pool is generally
     * asynchronous, however future implementations might differ.
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
     * @return If return true, the pool is empty
     */
    boolean isEmpty();

    /**
     * When the object factory create a new object, the created object size increase.
     * When the object is destroy, the created object size decrease.
     * If the created object size less then pool size, the pool will create a new object,
     * or else the pool will wait the object return
     * @return
     */
    int getCreatedObjectSize();

    /**
     * Represents the functionality to validate an object of the pool
     *
     * @param <T> The pooled object
     */
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
    interface ObjectFactory<T> {

        /**
         * Create a new object in future.
         *
         * @return a future that is a new instance of an object of type T.
         */
        CompletableFuture<PooledObject<T>> createNew(Pool<T> pool);
    }
}
