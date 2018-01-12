package test.codec.websocket;

import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ByteBufferAssert {
    public static void assertEquals(String message, byte[] expected, byte[] actual) {
        assertThat(message + " byte[].length", actual.length, is(expected.length));
        int len = expected.length;
        for (int i = 0; i < len; i++) {
            assertThat(message + " byte[" + i + "]", actual[i], is(expected[i]));
        }
    }

    public static void assertEquals(String message, ByteBuffer expectedBuffer, ByteBuffer actualBuffer) {
        if (expectedBuffer == null) {
            assertThat(message, actualBuffer, nullValue());
        } else {
            byte expectedBytes[] = BufferUtils.toArray(expectedBuffer);
            byte actualBytes[] = BufferUtils.toArray(actualBuffer);
            assertEquals(message, expectedBytes, actualBytes);
        }
    }

    public static void assertEquals(String message, String expectedString, ByteBuffer actualBuffer) {
        String actualString = BufferUtils.toString(actualBuffer);
        assertThat(message, actualString, is(expectedString));
    }

    public static void assertSize(String message, int expectedSize, ByteBuffer buffer) {
        if ((expectedSize == 0) && (buffer == null)) {
            return;
        }
        assertThat(message + " buffer.remaining", buffer.remaining(), is(expectedSize));
    }
}
