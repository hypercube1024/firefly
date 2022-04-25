package com.fireflysource.net.udp;

import kotlinx.coroutines.CompletableJob;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.CoroutineScope;

import java.util.concurrent.Executor;

public interface UdpCoroutineDispatcher extends Executor {

    /**
     * Get the coroutine dispatcher of this connection. One TCP connection is always in the same coroutine context.
     *
     * @return The coroutine dispatcher of this connection.
     */
    CoroutineDispatcher getCoroutineDispatcher();

    /**
     * Get the coroutine scope of this connection.
     *
     * @return The coroutine scope.
     */
    CoroutineScope getCoroutineScope();

    /**
     * Get the supervisor job.
     *
     * @return The supervisor job.
     */
    CompletableJob getSupervisorJob();
}
