package com.fireflysource.net.websocket.model;

import com.fireflysource.common.sys.Result;
import com.fireflysource.net.websocket.frame.Frame;

import java.util.function.Consumer;

/**
 * Interface for dealing with frames outgoing to (eventually) the network layer.
 */
public interface OutgoingFrames {
    /**
     * A frame, and optional callback, intended for the network layer.
     * <p>
     * Note: the frame can undergo many transformations in the various
     * layers and extensions present in the implementation.
     * <p>
     * If you are implementing a mutation, you are obliged to handle
     * the incoming WriteCallback appropriately.
     *
     * @param frame  the frame to eventually write to the network layer.
     * @param result the callback to notify when the frame is written.
     */
    void outgoingFrame(Frame frame, Consumer<Result<Void>> result);
}
