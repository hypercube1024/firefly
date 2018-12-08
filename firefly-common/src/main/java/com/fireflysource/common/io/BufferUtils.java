package com.fireflysource.common.io;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Pengtao Qiu
 */
abstract public class BufferUtils {

    /**
     * Convert a ByteBuffer to a byte array.
     *
     * @param buffer The buffer to convert in flush mode. The buffer is not altered.
     * @return An array of bytes duplicated from the buffer.
     */
    public static byte[] toArray(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            byte[] array = buffer.array();
            int from = buffer.arrayOffset() + buffer.position();
            return Arrays.copyOfRange(array, from, from + buffer.remaining());
        } else {
            byte[] to = new byte[buffer.remaining()];
            buffer.slice().get(to);
            return to;
        }
    }
}
