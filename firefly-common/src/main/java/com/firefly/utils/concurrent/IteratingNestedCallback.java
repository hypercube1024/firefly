package com.firefly.utils.concurrent;

/**
 * Iterating Nested Callback.
 * <p>
 * This specialized callback is used when breaking up an asynchronous task into
 * smaller asynchronous tasks. A typical pattern is that a successful callback
 * is used to schedule the next sub task, but if that task completes quickly and
 * uses the calling thread to callback the success notification, this can result
 * in a growing stack depth.
 * </p>
 * <p>
 * To avoid this issue, this callback uses an AtomicBoolean to note if the
 * success callback has been called during the processing of a sub task, and if
 * so then the processing iterates rather than recurses.
 * </p>
 * <p>
 * This callback is passed to the asynchronous handling of each sub task and a
 * call the {@link #succeeded()} on this call back represents completion of the
 * subtask. Only once all the subtasks are completed is the
 * {@link Callback#succeeded()} method called on the {@link Callback} instance
 * passed the the {@link #IteratingNestedCallback(Callback)} constructor.
 * </p>
 */
public abstract class IteratingNestedCallback extends IteratingCallback {
    final Callback _callback;

    public IteratingNestedCallback(Callback callback) {
        _callback = callback;
    }

    @Override
    public boolean isNonBlocking() {
        return _callback.isNonBlocking();
    }

    @Override
    protected void onCompleteSuccess() {
        _callback.succeeded();
    }

    @Override
    protected void onCompleteFailure(Throwable x) {
        _callback.failed(x);
    }

    @Override
    public String toString() {
        return String.format("%s@%x", getClass().getSimpleName(), hashCode());
    }
}
