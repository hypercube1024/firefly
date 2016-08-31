package com.firefly.utils.lang.pool;

import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * Represents an abstract pool, that defines the procedure of returning an
 * object to the pool.
 */
abstract public class AbstractPool<T> extends AbstractLifeCycle implements Pool<T> {

	/**
	 * Returns the object to the pool. The method first validates the object if
	 * it is re-usable and then puts returns it to the pool.
	 *
	 * If the object validation fails, some implementations will try to create a
	 * new one and put it into the pool; however this behaviour is subject to
	 * change from implementation to implementation
	 *
	 */
	@Override
	public void release(T t) {
		if (isValid(t)) {
			returnToPool(t);
		} else {
			handleInvalidReturn(t);
		}
	}

	protected abstract void handleInvalidReturn(T t);

	protected abstract void returnToPool(T t);

	protected abstract boolean isValid(T t);

}
