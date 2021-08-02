package com.fireflysource.common.actor;

/**
 * The actor dispatcher.
 */
public interface Dispatcher {

    /**
     * Dispatch the message process task.
     *
     * @param runnable The message process task.
     */
    void dispatch(Runnable runnable);
}
