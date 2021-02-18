package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;
import kotlinx.coroutines.CoroutineDispatcher;

import java.nio.channels.AsynchronousChannelGroup;

/**
 * The asynchronous channel and message thread group. It manages the IO and message threads.
 *
 * @author Pengtao Qiu
 */
public interface TcpChannelGroup extends LifeCycle {

    /**
     * Get the asynchronous channel group. It manages the IO thread.
     *
     * @return The asynchronous channel group.
     */
    AsynchronousChannelGroup getAsynchronousChannelGroup();

    /**
     * Get the coroutine dispatcher. It is the message handler thread pool.
     *
     * @param connectionId The connection id.
     * @return The coroutine dispatcher.
     */
    CoroutineDispatcher getDispatcher(int connectionId);

    /**
     * Get the next connection id. It is auto increment.
     *
     * @return The next connection id.
     */
    int getNextId();

}
