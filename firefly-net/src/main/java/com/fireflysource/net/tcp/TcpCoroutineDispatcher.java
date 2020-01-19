package com.fireflysource.net.tcp;

import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.CoroutineScope;

public interface TcpCoroutineDispatcher {

    /**
     * Get the coroutine dispatcher of this connection. One TCP connection is always in the same coroutine context.
     *
     * @return The coroutine dispatcher of this connection.
     */
    CoroutineDispatcher getCoroutineDispatcher();

    /**
     * Execute a task in the current TCP message process thread.
     *
     * @param runnable The runnable task.
     */
    void execute(Runnable runnable);

    /**
     * Get the coroutine scope of this connection.
     *
     * @return The coroutine scope.
     */
    CoroutineScope getCoroutineScope();
}
