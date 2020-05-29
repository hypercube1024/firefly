package com.fireflysource.net.websocket.utils;


import com.fireflysource.common.io.BufferUtils;
import org.junit.jupiter.api.Assertions;

import java.nio.ByteBuffer;


public class ByteBufferAssert {

    public static void assertEquals(String message, byte[] expected, byte[] actual) {
        Assertions.assertEquals(expected.length, actual.length);
        int len = expected.length;
        for (int i = 0; i < len; i++) {
            Assertions.assertEquals(expected[i], actual[i]);
        }
    }

    public static void assertEquals(ByteBuffer expectedBuffer, ByteBuffer actualBuffer, String message) {
        assertEquals(message, expectedBuffer, actualBuffer);
    }

    public static void assertEquals(String message, ByteBuffer expectedBuffer, ByteBuffer actualBuffer) {
        if (expectedBuffer == null) {
            Assertions.assertNull(actualBuffer);
        } else {
            byte[] expectedBytes = BufferUtils.toArray(expectedBuffer);
            byte[] actualBytes = BufferUtils.toArray(actualBuffer);
            assertEquals(message, expectedBytes, actualBytes);
        }
    }

    public static void assertEquals(String message, String expectedString, ByteBuffer actualBuffer) {
        String actualString = BufferUtils.toString(actualBuffer);
        Assertions.assertEquals(expectedString, actualString);
    }

    public static void assertSize(String message, int expectedSize, ByteBuffer buffer) {
        if ((expectedSize == 0) && (buffer == null)) {
            return;
        }
        Assertions.assertEquals(expectedSize, buffer.remaining());
    }
}
