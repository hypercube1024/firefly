package com.firefly.net.tcp.secure.openssl;

import java.nio.ByteBuffer;

/**
 * {@link OutOfMemoryError} that is throws if {@link PlatformDependent#allocateDirectNoCleaner(int)} can not allocate
 * a new {@link ByteBuffer} due memory restrictions.
 */
public final class OutOfDirectMemoryError extends OutOfMemoryError {
    private static final long serialVersionUID = 4228264016184011555L;

    OutOfDirectMemoryError(String s) {
        super(s);
    }
}
