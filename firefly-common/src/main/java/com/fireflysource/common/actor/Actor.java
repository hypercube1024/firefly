package com.fireflysource.common.actor;

/**
 * The actor interface.
 *
 * @param <T> The actor message type.
 */
public interface Actor<T> {

    /**
     * Get actor id.
     *
     * @return The actor id.
     */
    String getAddress();

    /**
     * Send message to this actor.
     *
     * @param message The message.
     * @return If true, send message success.
     */
    boolean send(T message);

}
