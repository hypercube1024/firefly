package com.firefly.utils.concurrent;

import java.util.concurrent.CompletableFuture;

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
	default void succeeded(C result) {
	}

	/**
	 * <p>
	 * Callback invoked when the operation fails.
	 * </p>
	 *
	 * @param x
	 *            the reason for the operation failure
	 */
	default void failed(Throwable x) {
	}

	/**
	 * <p>
	 * Empty implementation of {@link Promise}.
	 * </p>
	 *
	 * @param <U>
	 *            the type of the result
	 */
	class Adapter<U> implements Promise<U> {
		@Override
		public void succeeded(U result) {
		}

		@Override
		public void failed(Throwable x) {
			x.printStackTrace();
		}
	}

	/**
	 * <p>
	 * Creates a promise from the given incomplete CompletableFuture.
	 * </p>
	 * <p>
	 * When the promise completes, either succeeding or failing, the
	 * CompletableFuture is also completed, respectively via
	 * {@link CompletableFuture#complete(Object)} or
	 * {@link CompletableFuture#completeExceptionally(Throwable)}.
	 * </p>
	 *
	 * @param completable
	 *            the CompletableFuture to convert into a promise
	 * @return a promise that when completed, completes the given
	 *         CompletableFuture
	 * @param <T>
	 *            the type of the result
	 */
	@SuppressWarnings("unchecked")
	static <T> Promise<T> from(CompletableFuture<? super T> completable) {
		if (completable instanceof Promise)
			return (Promise<T>) completable;

		return new Promise<T>() {
			@Override
			public void succeeded(T result) {
				completable.complete(result);
			}

			@Override
			public void failed(Throwable x) {
				completable.completeExceptionally(x);
			}
		};
	}

	/**
	 * <p>
	 * A CompletableFuture that is also a Promise.
	 * </p>
	 *
	 * @param <S>
	 *            the type of the result
	 */
	class Completable<S> extends CompletableFuture<S> implements Promise<S> {
		@Override
		public void succeeded(S result) {
			complete(result);
		}

		@Override
		public void failed(Throwable x) {
			completeExceptionally(x);
		}
	}
}
