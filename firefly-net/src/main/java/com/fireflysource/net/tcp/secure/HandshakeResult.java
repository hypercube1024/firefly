package com.fireflysource.net.tcp.secure;

import java.nio.ByteBuffer;
import java.util.List;

public interface HandshakeResult {

    /**
     * Get the stashed application buffers during the handshake process.
     *
     * @return The stashed application buffers.
     */
    List<ByteBuffer> getStashedAppBuffers();

    /**
     * The application protocol negotiation result.
     *
     * @return The application protocol negotiation result.
     */
    String getApplicationProtocol();

}
