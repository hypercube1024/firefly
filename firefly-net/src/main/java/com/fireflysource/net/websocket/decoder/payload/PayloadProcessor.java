package com.fireflysource.net.websocket.decoder.payload;


import com.fireflysource.net.websocket.exception.BadPayloadException;
import com.fireflysource.net.websocket.frame.Frame;

import java.nio.ByteBuffer;

/**
 * Process the payload (for demasking, validating, etc..)
 */
public interface PayloadProcessor {
    /**
     * Used to process payloads for in the spec.
     *
     * @param payload the payload to process
     * @throws BadPayloadException the exception when the payload fails to validate properly
     */
    void process(ByteBuffer payload);

    void reset(Frame frame);
}
