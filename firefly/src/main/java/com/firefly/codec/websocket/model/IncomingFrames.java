package com.firefly.codec.websocket.model;

import com.firefly.codec.websocket.frame.Frame;

/**
 * Interface for dealing with Incoming Frames.
 */
public interface IncomingFrames {
    void incomingError(Throwable t);

    /**
     * Process the incoming frame.
     * <p>
     * Note: if you need to hang onto any information from the frame, be sure
     * to copy it, as the information contained in the Frame will be released
     * and/or reused by the implementation.
     *
     * @param frame the frame to process
     */
    void incomingFrame(Frame frame);
}
