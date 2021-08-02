package com.fireflysource.common.actor;

/**
 * The actor mailbox.
 */
public interface Mailbox<U, S> {

    S pollSystemMessage();

    boolean offerSystemMessage(S systemMessage);

    boolean hasSystemMessage();

    U pollUserMessage();

    boolean offerUserMessage(U userMessage);

    boolean hasUserMessage();
}
