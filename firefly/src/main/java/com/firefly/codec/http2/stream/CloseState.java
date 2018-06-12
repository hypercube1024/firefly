package com.firefly.codec.http2.stream;

/**
 * The set of close states for a stream or a session.
 * <pre>
 *                rcv hc
 * NOT_CLOSED ---------------&gt; REMOTELY_CLOSED
 *      |                             |
 *   gen|                             |gen
 *    hc|                             |hc
 *      |                             |
 *      v              rcv hc         v
 * LOCALLY_CLOSING --------------&gt; CLOSING
 *      |                             |
 *   snd|                             |gen
 *    hc|                             |hc
 *      |                             |
 *      v              rcv hc         v
 * LOCALLY_CLOSED ----------------&gt; CLOSED
 * </pre>
 */
public enum CloseState {
    /**
     * Fully open.
     */
    NOT_CLOSED,
    /**
     * A half-close frame has been generated.
     */
    LOCALLY_CLOSING,
    /**
     * A half-close frame has been generated and sent.
     */
    LOCALLY_CLOSED,
    /**
     * A half-close frame has been received.
     */
    REMOTELY_CLOSED,
    /**
     * A half-close frame has been received and a half-close frame has been generated, but not yet sent.
     */
    CLOSING,
    /**
     * Fully closed.
     */
    CLOSED;

    public enum Event {
        RECEIVED,
        BEFORE_SEND,
        AFTER_SEND
    }
}
