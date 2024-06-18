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
     * Offer message to this actor's mailbox.
     *
     * @param message The message.
     * @return If true, offer message success.
     */
    boolean offer(T message);

}
