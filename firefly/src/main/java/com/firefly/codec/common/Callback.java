package com.firefly.codec.common;

/**
 * <p>
 * A callback abstraction that handles completed/failed events of asynchronous
 * operations.
 * </p>
 *
 * <p>
 * Semantically this is equivalent to an optimise Promise&lt;Void&gt;, but
 * callback is a more meaningful name than EmptyPromise
 * </p>
 */
public interface Callback {
	/**
	 * Instance of Adapter that can be used when the callback methods need an
	 * empty implementation without incurring in the cost of allocating a new
	 * Adapter object.
	 */
	public static Callback NOOP = new Callback() {

		@Override
		public void succeeded() {
		}

		@Override
		public void failed(Throwable x) {
		}

		@Override
		public boolean isNonBlocking() {
			return false;
		}
	};

	/**
	 * <p>
	 * Callback invoked when the operation completes.
	 * </p>
	 *
	 * @see #failed(Throwable)
	 */
	public void succeeded();

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
	 * @return True if the callback is known to never block the caller
	 */
	public boolean isNonBlocking();

	/**
	 * Callback interface that declares itself as non-blocking
	 */
	public static abstract class NonBlocking implements Callback {
		@Override
		public boolean isNonBlocking() {
			return true;
		}
	}

}
