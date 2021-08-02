package com.fireflysource.common.actor;

/**
 * Only call these methods in the receiving message thread.
 */
public interface ActorInternalApi {

    /**
     * Pause to receive messages.
     */
    void pause();

    /**
     * Resume to receive messages.
     */
    void resume();

    /**
     * Shutdown the actor.
     */
    void shutdown();

    /**
     * Restart the actor.
     */
    void restart();

    /**
     * Get the actor internal state.
     *
     * @return The actor internal state.
     */
    ActorState getActorState();
}
