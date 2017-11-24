package com.firefly.codec.websocket.model;

import com.firefly.codec.websocket.frame.Frame;

/**
 * The possible batch modes when invoking {@link OutgoingFrames#outgoingFrame(Frame, WriteCallback, BatchMode)}.
 */
public enum BatchMode {
    /**
     * Implementers are free to decide whether to send or not frames
     * to the network layer.
     */
    AUTO,

    /**
     * Implementers must batch frames.
     */
    ON,

    /**
     * Implementers must send frames to the network layer.
     */
    OFF;

    public static BatchMode max(BatchMode one, BatchMode two) {
        // Return the BatchMode that has the higher priority, where AUTO < ON < OFF.
        return one.ordinal() < two.ordinal() ? two : one;
    }
}
