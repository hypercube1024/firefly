package com.firefly.codec.websocket.stream;

/**
 * Connection suspend token
 */
public interface SuspendToken {
    /**
     * Resume a previously suspended connection.
     */
    void resume();
}
