package com.fireflysource.net.tcp;

import kotlinx.coroutines.CoroutineDispatcher;

public interface TcpCoroutineDispatcher {

    /**
     * Get the coroutine dispatcher of this connection. One TCP connection is always in the same coroutine context.
     *
     * @return The coroutine dispatcher of this connection.
     */
    CoroutineDispatcher getCoroutineDispatcher();
}
