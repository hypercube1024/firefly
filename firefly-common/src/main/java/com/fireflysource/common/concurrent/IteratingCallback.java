package com.fireflysource.common.concurrent;

import com.fireflysource.common.sys.Result;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * This specialized callback implements a pattern that allows
 * a large job to be broken into smaller tasks using iteration
 * rather than recursion.
 * <p>
 * A typical example is the write of a large content to a socket,
 * divided in chunks. Chunk C1 is written by thread T1, which
 * also invokes the callback, which writes chunk C2, which invokes
 * the callback again, which writes chunk C3, and so forth.
 * </p>
 * <p>
 * The problem with the example is that if the callback thread
 * is the same that performs the I/O operation, then the process
 * is recursive and may result in a stack overflow.
 * To avoid the stack overflow, a thread dispatch must be performed,
 * causing context switching and cache misses, affecting performance.
 * </p>
 * <p>
 * To avoid this issue, this callback uses an AtomicReference to
 * record whether success callback has been called during the processing
 * of a sub task, and if so then the processing iterates rather than
 * recurring.
 * </p>
 * <p>
 * Subclasses must implement method {@link #process()} where the sub
 * task is executed and a suitable {@link Action} is
 * returned to this callback to indicate the overall progress of the job.
 * This callback is passed to the asynchronous execution of each sub
 * task and a call the success result on this callback represents
 * the completion of the sub task.
 * </p>
 */
public abstract class IteratingCallback implements Consumer<Result<Void>> {
    /**
     * The internal states of this callback
     */
    private enum State {
        /**
         * This callback is IDLE, ready to iterate.
         */
        IDLE,

        /**
         * This callback is iterating calls to {@link #process()} and is dealing with
         * the returns.  To get into processing state, it much of held the lock state
         * and set iterating to true.
         */
        PROCESSING,

        /**
         * Waiting for a schedule callback
         */
        PENDING,

        /**
         * Called by a schedule callback
         */
        CALLED,

        /**
         * The overall job has succeeded as indicated by a {@link Action#SUCCEEDED} return
         * from {@link IteratingCallback#process()}
         */
        SUCCEEDED,

        /**
         * The overall job has failed as indicated
         */
        FAILED,

        /**
         * This callback has been closed and cannot be reset.
         */
        CLOSED
    }

    /**
     * The indication of the overall progress of the overall job that
     * implementations of {@link #process()} must return.
     */
    protected enum Action {
        /**
         * Indicates that {@link #process()} has no more work to do,
         * but the overall job is not completed yet, probably waiting
         * for additional events to trigger more work.
         */
        IDLE,
        /**
         * Indicates that {@link #process()} is executing asynchronously
         * a sub task, where the execution has started but the callback
         * may have not yet been invoked.
         */
        SCHEDULED,

        /**
         * Indicates that {@link #process()} has completed the overall job.
         */
        SUCCEEDED
    }

    private Locker locker = new Locker();
    private State state;
    private boolean iterate;

    protected IteratingCallback() {
        state = State.IDLE;
    }

    protected IteratingCallback(boolean needReset) {
        state = needReset ? State.SUCCEEDED : State.IDLE;
    }

    /**
     * Method called by {@link #iterate()} to process the sub task.
     * <p>
     * Implementations must start the asynchronous execution of the sub task
     * (if any) and return an appropriate action:
     * </p>
     * <ul>
     * <li>{@link Action#IDLE} when no sub tasks are available for execution
     * but the overall job is not completed yet</li>
     * <li>{@link Action#SCHEDULED} when the sub task asynchronous execution
     * has been started</li>
     * <li>{@link Action#SUCCEEDED} when the overall job is completed</li>
     * </ul>
     *
     * @return the appropriate Action
     * @throws Throwable if the sub task processing throws
     */
    protected abstract Action process() throws Throwable;

    /**
     * Invoked when the overall task has completed successfully.
     *
     * @see #onCompleteFailure(Throwable)
     */
    protected void onCompleteSuccess() {
    }

    /**
     * Invoked when the overall task has completed with a failure.
     *
     * @param cause the throwable to indicate cause of failure
     * @see #onCompleteSuccess()
     */
    protected void onCompleteFailure(Throwable cause) {
    }

    /**
     * This method must be invoked by applications to start the processing
     * of sub tasks.  It can be called at any time by any thread, and it's
     * contract is that when called, then the {@link #process()} method will
     * be called during or soon after, either by the calling thread or by
     * another thread.
     */
    public void iterate() {
        boolean process = false;

        loop:
        while (true) {
            try (Locker.Lock lock = locker.lock()) {
                switch (state) {
                    case PENDING:
                    case CALLED:
                        // process will be called when callback is handled
                        break loop;

                    case IDLE:
                        state = State.PROCESSING;
                        process = true;
                        break loop;

                    case PROCESSING:
                        iterate = true;
                        break loop;

                    case FAILED:
                    case SUCCEEDED:
                        break loop;

                    case CLOSED:
                    default:
                        throw new IllegalStateException(toString());
                }
            }
        }
        if (process)
            processing();
    }

    private void processing() {
        // This should only ever be called when in processing state, however a failed or close call
        // may happen concurrently, so state is not assumed.

        boolean onCompleteSuccess = false;

        // While we are processing
        processing:
        while (true) {
            // Call process to get the action that we have to take.
            Action action;
            try {
                action = process();
            } catch (Throwable x) {
                accept(Result.createFailedResult(x));
                break processing;
            }

            // acted on the action we have just received
            try (Locker.Lock lock = locker.lock()) {
                switch (state) {
                    case PROCESSING: {
                        switch (action) {
                            case IDLE: {
                                // Has iterate been called while we were processing?
                                if (iterate) {
                                    // yes, so skip idle and keep processing
                                    iterate = false;
                                    state = State.PROCESSING;
                                    continue processing;
                                }

                                // No, so we can go idle
                                state = State.IDLE;
                                break processing;
                            }

                            case SCHEDULED: {
                                // we won the race against the callback, so the callback has to process and we can break processing
                                state = State.PENDING;
                                break processing;
                            }

                            case SUCCEEDED: {
                                // we lost the race against the callback,
                                iterate = false;
                                state = State.SUCCEEDED;
                                onCompleteSuccess = true;
                                break processing;
                            }

                            default:
                                break;
                        }
                        throw new IllegalStateException(String.format("%s[action=%s]", this, action));
                    }

                    case CALLED: {
                        switch (action) {
                            case SCHEDULED: {
                                // we lost the race, so we have to keep processing
                                state = State.PROCESSING;
                                continue processing;
                            }

                            default:
                                throw new IllegalStateException(String.format("%s[action=%s]", this, action));
                        }
                    }

                    case SUCCEEDED:
                    case FAILED:
                    case CLOSED:
                        break processing;

                    case IDLE:
                    case PENDING:
                    default:
                        throw new IllegalStateException(String.format("%s[action=%s]", this, action));
                }
            }
        }

        if (onCompleteSuccess)
            onCompleteSuccess();
    }

    @Override
    public void accept(Result<Void> result) {
        if (result.isSuccess()) {
            this.success();
        } else {
            this.failure(result.getThrowable());
        }
    }

    /**
     * Invoked when the sub task succeeds.
     */
    private void success() {
        boolean process = false;
        try (Locker.Lock lock = locker.lock()) {
            switch (state) {
                case PROCESSING: {
                    state = State.CALLED;
                    break;
                }
                case PENDING: {
                    state = State.PROCESSING;
                    process = true;
                    break;
                }
                case CLOSED:
                case FAILED: {
                    // Too late!
                    break;
                }
                default: {
                    throw new IllegalStateException(toString());
                }
            }
        }
        if (process)
            processing();
    }


    /**
     * Invoked when the sub task fails.
     */
    private void failure(Throwable x) {
        boolean failure = false;
        try (Locker.Lock lock = locker.lock()) {
            switch (state) {
                case SUCCEEDED:
                case FAILED:
                case IDLE:
                case CLOSED:
                case CALLED:
                    // too late!.
                    break;

                case PENDING:
                case PROCESSING: {
                    state = State.FAILED;
                    failure = true;
                    break;
                }
                default:
                    throw new IllegalStateException(toString());
            }
        }
        if (failure)
            onCompleteFailure(x);
    }

    public void close() {
        String failure = null;
        try (Locker.Lock lock = locker.lock()) {
            switch (state) {
                case IDLE:
                case SUCCEEDED:
                case FAILED:
                    state = State.CLOSED;
                    break;

                case CLOSED:
                    break;

                default:
                    failure = String.format("Close %s in state %s", this, state);
                    state = State.CLOSED;
            }
        }

        if (failure != null)
            onCompleteFailure(new IOException(failure));
    }

    /*
     * only for testing
     * @return whether this callback is idle and {@link #iterate()} needs to be called
     */
    boolean isIdle() {
        try (Locker.Lock lock = locker.lock()) {
            return state == State.IDLE;
        }
    }

    public boolean isClosed() {
        try (Locker.Lock lock = locker.lock()) {
            return state == State.CLOSED;
        }
    }

    /**
     * @return whether this callback has failed
     */
    public boolean isFailed() {
        try (Locker.Lock lock = locker.lock()) {
            return state == State.FAILED;
        }
    }

    /**
     * @return whether this callback has succeeded
     */
    public boolean isSucceeded() {
        try (Locker.Lock lock = locker.lock()) {
            return state == State.SUCCEEDED;
        }
    }

    /**
     * Resets this callback.
     * <p>
     * A callback can only be reset to IDLE from the
     * SUCCEEDED or FAILED states or if it is already IDLE.
     * </p>
     *
     * @return true if the reset was successful
     */
    public boolean reset() {
        try (Locker.Lock lock = locker.lock()) {
            switch (state) {
                case IDLE:
                    return true;

                case SUCCEEDED:
                case FAILED:
                    iterate = false;
                    state = State.IDLE;
                    return true;

                default:
                    return false;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", super.toString(), state);
    }
}
