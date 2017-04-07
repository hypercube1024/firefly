package com.firefly.utils.concurrent;

import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * A callback abstraction that handles completed/failed events of asynchronous
 * operations.
 * </p>
 * <p>
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
    Callback NOOP = new Callback() {
    };

    /**
     * <p>
     * Callback invoked when the operation completes.
     * </p>
     *
     * @see #failed(Throwable)
     */
    default void succeeded() {
    }

    /**
     * <p>
     * Callback invoked when the operation fails.
     * </p>
     *
     * @param x the reason for the operation failure
     */
    default void failed(Throwable x) {
    }

    /**
     * @return True if the callback is known to never block the caller
     */
    default boolean isNonBlocking() {
        return false;
    }

    /**
     * <p>
     * Creates a non-blocking callback from the given incomplete
     * CompletableFuture.
     * </p>
     * <p>
     * When the callback completes, either succeeding or failing, the
     * CompletableFuture is also completed, respectively via
     * {@link CompletableFuture#complete(Object)} or
     * {@link CompletableFuture#completeExceptionally(Throwable)}.
     * </p>
     *
     * @param completable the CompletableFuture to convert into a callback
     * @return a callback that when completed, completes the given
     * CompletableFuture
     */
    static Callback from(CompletableFuture<?> completable) {
        return from(completable, false);
    }

    /**
     * <p>
     * Creates a callback from the given incomplete CompletableFuture, with the
     * given {@code blocking} characteristic.
     * </p>
     *
     * @param completable the CompletableFuture to convert into a callback
     * @param blocking    whether the callback is blocking
     * @return a callback that when completed, completes the given
     * CompletableFuture
     */
    static Callback from(CompletableFuture<?> completable, boolean blocking) {
        if (completable instanceof Callback)
            return (Callback) completable;

        return new Callback() {
            @Override
            public void succeeded() {
                completable.complete(null);
            }

            @Override
            public void failed(Throwable x) {
                completable.completeExceptionally(x);
            }

            @Override
            public boolean isNonBlocking() {
                return !blocking;
            }
        };
    }

    /**
     * Callback interface that declares itself as non-blocking
     */
    interface NonBlocking extends Callback {
        @Override
        default boolean isNonBlocking() {
            return true;
        }
    }

    class Nested implements Callback {
        private final Callback callback;

        public Nested(Callback callback) {
            this.callback = callback;
        }

        public Nested(Nested nested) {
            this.callback = nested.callback;
        }

        public Callback getCallback() {
            return callback;
        }

        @Override
        public void succeeded() {
            callback.succeeded();
        }

        @Override
        public void failed(Throwable x) {
            callback.failed(x);
        }

        @Override
        public boolean isNonBlocking() {
            return callback.isNonBlocking();
        }
    }

    /**
     * <p>
     * A CompletableFuture that is also a Callback.
     * </p>
     */
    class Completable extends CompletableFuture<Void> implements Callback {
        private final boolean blocking;

        public Completable() {
            this(false);
        }

        public Completable(boolean blocking) {
            this.blocking = blocking;
        }

        @Override
        public void succeeded() {
            complete(null);
        }

        @Override
        public void failed(Throwable x) {
            completeExceptionally(x);
        }

        @Override
        public boolean isNonBlocking() {
            return !blocking;
        }
    }
}
