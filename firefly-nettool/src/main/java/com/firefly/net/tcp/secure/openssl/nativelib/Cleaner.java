package com.firefly.net.tcp.secure.openssl.nativelib;

import java.nio.ByteBuffer;

/**
 * Allows to free direct {@link ByteBuffer}s.
 */
interface Cleaner {

    /**
     * Free a direct {@link ByteBuffer} if possible
     */
    void freeDirectBuffer(ByteBuffer buffer);
}
