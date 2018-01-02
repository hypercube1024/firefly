package com.firefly.codec.websocket.model;

/**
 * Callback for Write events.
 */
public interface WriteCallback {

    /**
     * <p>
     * Callback invoked when the write fails.
     * </p>
     *
     * @param x the reason for the write failure
     */
    void writeFailed(Throwable x);

    /**
     * <p>
     * Callback invoked when the write completes.
     * </p>
     *
     * @see #writeFailed(Throwable)
     */
    void writeSuccess();
}
