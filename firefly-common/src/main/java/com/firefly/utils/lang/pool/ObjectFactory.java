package com.firefly.utils.lang.pool;

/**
 * Represents the mechanism to create new objects to be used in an object pool.
 */
public interface ObjectFactory<T> {

	/**
	 * Returns a new instance of an object of type T.
	 *
	 * @return T an new instance of the object of type T
	 */
	T createNew();
}
