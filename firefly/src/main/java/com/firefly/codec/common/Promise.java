package com.firefly.codec.common;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * <p>
 * A callback abstraction that handles completed/failed events of asynchronous
 * operations.
 * </p>
 *
 * @param <C>
 *            the type of the context object
 */
public interface Promise<C> {
	/**
	 * <p>
	 * Callback invoked when the operation completes.
	 * </p>
	 *
	 * @param result
	 *            the context
	 * @see #failed(Throwable)
	 */
	public abstract void succeeded(C result);

	/**
	 * <p>
	 * Callback invoked when the operation fails.
	 * </p>
	 *
	 * @param x
	 *            the reason for the operation failure
	 */
	public void failed(Throwable x);

	/**
	 * <p>
	 * Empty implementation of {@link Promise}
	 * </p>
	 *
	 * @param <C>
	 *            the type of the context object
	 */
	public static class Adapter<C> implements Promise<C> {
		private static Log log = LogFactory.getInstance().getLog("firefly-system");

		@Override
		public void succeeded(C result) {
		}

		@Override
		public void failed(Throwable x) {
			log.error("promise error", x);
		}
	}

}
