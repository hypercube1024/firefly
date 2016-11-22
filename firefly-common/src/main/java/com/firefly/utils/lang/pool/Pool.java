package com.firefly.utils.lang.pool;

import com.firefly.utils.lang.LifeCycle;

/**
 * Represents a cached pool of objects.
 */
public interface Pool<T> extends LifeCycle {

	/**
	 * Returns an instance from the pool. The call may be a blocking one or a
	 * non-blocking one and that is determined by the internal implementation.
	 *
	 * If the call is a blocking call, the call returns immediately with a valid
	 * object if available, else the thread is made to wait until an object
	 * becomes available. In case of a blocking call, it is advised that clients
	 * react to {@link InterruptedException} which might be thrown when the
	 * thread waits for an object to become available.
	 *
	 * If the call is a non-blocking one, the call returns immediately
	 * irrespective of whether an object is available or not. If any object is
	 * available the call returns it else the call returns null.
	 *
	 * The validity of the objects are determined using the Validator interface,
	 * such that an object o is valid if Validator.isValid(o) == true
	 *
	 * @return T one of the pooled objects.
	 */
	T get();

	/**
	 * Releases the object and puts it back to the pool.
	 *
	 * The mechanism of putting the object back to the pool is generally
	 * asynchronous, however future implementations might differ.
	 *
	 * @param t
	 *            the object to return to the pool
	 */

	void release(T t);
	
	int size();
	
	boolean isEmpty();

	/**
	 * Represents the functionality to validate an object of the pool
	 */
	interface Validator<T> {
		/**
		 * Checks whether the object is valid.
		 *
		 * @param t
		 *            the object to check.
		 *
		 * @return true if the object is valid else false.
		 */
		boolean isValid(T t);

	}

	interface Dispose<T> {
		/**
		 * Performs any cleanup activities before discarding the object. For
		 * example before discarding database connection objects, the pool will
		 * want to close the connections.
		 *
		 * @param t
		 *            the object to cleanup
		 */

		void destroy(T t);
	}
}
