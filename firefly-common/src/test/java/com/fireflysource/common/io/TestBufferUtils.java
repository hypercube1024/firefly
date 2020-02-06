package com.fireflysource.common.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pengtao Qiu
 */
class TestBufferUtils {

    @Test
    void testToArray() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(6);
        buffer.flip();

        byte[] bytes = BufferUtils.toArray(buffer);
        assertNotSame(bytes, buffer.array());
        assertEquals(6, ByteBuffer.wrap(bytes).getInt());

        buffer = ByteBuffer.allocateDirect(4);
        buffer.putInt(3);
        buffer.flip();
        bytes = BufferUtils.toArray(buffer);
        assertEquals(3, ByteBuffer.wrap(bytes).getInt());
    }

    @Test
    void testCollectionToArray() {
        int count = 10;
        List<ByteBuffer> list = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(i);
            buffer.flip();
            list.add(buffer);
        }

        byte[] bytes = BufferUtils.toArray(list);
        assertEquals(count * 4, bytes.length);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < count; i++) {
            int x = buffer.getInt();
            assertEquals(i, x);
        }
    }

    @Test
    void testToInt() {
        ByteBuffer[] buf = {
                BufferUtils.toBuffer("0"),
                BufferUtils.toBuffer(" 42 "),
                BufferUtils.toBuffer("   43abc"),
                BufferUtils.toBuffer("-44"),
                BufferUtils.toBuffer(" - 45;"),
                BufferUtils.toBuffer("-2147483648"),
                BufferUtils.toBuffer("2147483647"),
        };

        int[] val = {
                0, 42, 43, -44, -45, -2147483648, 2147483647
        };

        for (int i = 0; i < buf.length; i++)
            assertEquals(val[i], BufferUtils.toInt(buf[i]), "t" + i);
    }

    @Test
    void testPutInt() {
        int[] val = {
                0, 42, 43, -44, -45, Integer.MIN_VALUE, Integer.MAX_VALUE
        };

        String[] str = {
                "0", "42", "43", "-44", "-45", "" + Integer.MIN_VALUE, "" + Integer.MAX_VALUE
        };

        ByteBuffer buffer = ByteBuffer.allocate(24);

        for (int i = 0; i < val.length; i++) {
            BufferUtils.clearToFill(buffer);
            BufferUtils.putDecInt(buffer, val[i]);
            BufferUtils.flipToFlush(buffer, 0);
            assertEquals(str[i], BufferUtils.toString(buffer), "t" + i);
        }
    }

    @Test
    void testPutLong() {
        long[] val = {
                0L, 42L, 43L, -44L, -45L, Long.MIN_VALUE, Long.MAX_VALUE
        };

        String[] str = {
                "0", "42", "43", "-44", "-45", "" + Long.MIN_VALUE, "" + Long.MAX_VALUE
        };

        ByteBuffer buffer = ByteBuffer.allocate(50);

        for (int i = 0; i < val.length; i++) {
            BufferUtils.clearToFill(buffer);
            BufferUtils.putDecLong(buffer, val[i]);
            BufferUtils.flipToFlush(buffer, 0);
            assertEquals(str[i], BufferUtils.toString(buffer), "t" + i);
        }
    }

    @Test
    void testPutHexInt() {
        int[] val = {
                0, 42, 43, -44, -45, -2147483648, 2147483647
        };

        String[] str = {
                "0", "2A", "2B", "-2C", "-2D", "-80000000", "7FFFFFFF"
        };

        ByteBuffer buffer = ByteBuffer.allocate(50);

        for (int i = 0; i < val.length; i++) {
            BufferUtils.clearToFill(buffer);
            BufferUtils.putHexInt(buffer, val[i]);
            BufferUtils.flipToFlush(buffer, 0);
            assertEquals(str[i], BufferUtils.toString(buffer), "t" + i);
        }
    }

    @Test
    void testPut() {
        ByteBuffer to = BufferUtils.allocate(10);
        ByteBuffer from = BufferUtils.toBuffer("12345");

        BufferUtils.clear(to);
        assertEquals(5, BufferUtils.append(to, from));
        assertTrue(BufferUtils.isEmpty(from));
        assertEquals("12345", BufferUtils.toString(to));

        from = BufferUtils.toBuffer("XX67890ZZ");
        from.position(2);

        assertEquals(5, BufferUtils.append(to, from));
        assertEquals(2, from.remaining());
        assertEquals("1234567890", BufferUtils.toString(to));

        from = BufferUtils.toBuffer("1234");
        to = BufferUtils.allocate(from.remaining());
        BufferUtils.append(to, from);
        assertEquals(0, to.position());
        assertEquals(4, to.limit());
    }


    @Test
    void testAppend() {
        ByteBuffer to = BufferUtils.allocate(8);
        ByteBuffer from = BufferUtils.toBuffer("12345");

        BufferUtils.append(to, from.array(), 0, 3);
        assertEquals("123", BufferUtils.toString(to));
        BufferUtils.append(to, from.array(), 3, 2);
        assertEquals("12345", BufferUtils.toString(to));

        assertThrows(BufferOverflowException.class, () -> {
            BufferUtils.append(to, from.array(), 0, 5);
        });
    }


    @Test
    void testPutDirect() {
        ByteBuffer to = BufferUtils.allocateDirect(10);
        ByteBuffer from = BufferUtils.toBuffer("12345");

        BufferUtils.clear(to);
        assertEquals(5, BufferUtils.append(to, from));
        assertTrue(BufferUtils.isEmpty(from));
        assertEquals("12345", BufferUtils.toString(to));

        from = BufferUtils.toBuffer("XX67890ZZ");
        from.position(2);

        assertEquals(5, BufferUtils.append(to, from));
        assertEquals(2, from.remaining());
        assertEquals("1234567890", BufferUtils.toString(to));
    }

    @Test
    void testToBuffer_Array() {
        byte[] arr = new byte[128];
        Arrays.fill(arr, (byte) 0x44);
        ByteBuffer buf = BufferUtils.toBuffer(arr);

        int count = 0;
        while (buf.remaining() > 0) {
            byte b = buf.get();
            assertEquals(b, 0x44);
            count++;
        }

        assertEquals(arr.length, count, "Count of bytes");
    }

    @Test
    void testToBuffer_ArrayOffsetLength() {
        byte[] arr = new byte[128];
        Arrays.fill(arr, (byte) 0xFF); // fill whole thing with FF
        int offset = 10;
        int length = 100;
        Arrays.fill(arr, offset, offset + length, (byte) 0x77); // fill partial with 0x77
        ByteBuffer buf = BufferUtils.toBuffer(arr, offset, length);

        int count = 0;
        while (buf.remaining() > 0) {
            byte b = buf.get();
            assertEquals(b, 0x77);
            count++;
        }

        assertEquals(length, count, "Count of bytes");
    }

    @Test
    void testWriteToWithBufferThatDoesNotExposeArrayAndSmallContent() throws IOException {
        int capacity = BufferUtils.TEMP_BUFFER_SIZE / 4;
        testWriteToWithBufferThatDoesNotExposeArray(capacity);
    }

    @Test
    void testWriteToWithBufferThatDoesNotExposeArrayAndContentLengthMatchingTempBufferSize() throws IOException {
        int capacity = BufferUtils.TEMP_BUFFER_SIZE;
        testWriteToWithBufferThatDoesNotExposeArray(capacity);
    }

    @Test
    void testWriteToWithBufferThatDoesNotExposeArrayAndContentSlightlyBiggerThanTwoTimesTempBufferSize()
            throws
            IOException {
        int capacity = BufferUtils.TEMP_BUFFER_SIZE * 2 + 1024;
        testWriteToWithBufferThatDoesNotExposeArray(capacity);
    }


    @Test
    void testEnsureCapacity() {
        ByteBuffer b = BufferUtils.toBuffer("Goodbye Cruel World");
        assertSame(b, BufferUtils.ensureCapacity(b, 0));
        assertSame(b, BufferUtils.ensureCapacity(b, 10));
        assertSame(b, BufferUtils.ensureCapacity(b, b.capacity()));


        ByteBuffer b1 = BufferUtils.ensureCapacity(b, 64);
        assertNotSame(b, b1);
        assertEquals(64, b1.capacity());
        assertEquals("Goodbye Cruel World", BufferUtils.toString(b1));

        b1.position(8);
        b1.limit(13);
        assertEquals("Cruel", BufferUtils.toString(b1));
        ByteBuffer b2 = b1.slice();
        assertEquals("Cruel", BufferUtils.toString(b2));
        System.err.println(BufferUtils.toDetailString(b2));
        assertEquals(8, b2.arrayOffset());
        assertEquals(5, b2.capacity());

        assertSame(b2, BufferUtils.ensureCapacity(b2, 5));

        ByteBuffer b3 = BufferUtils.ensureCapacity(b2, 64);
        assertNotSame(b2, b3);
        assertEquals(64, b3.capacity());
        assertEquals("Cruel", BufferUtils.toString(b3));
        assertEquals(0, b3.arrayOffset());

        assertEquals(19, b.remaining());
        b.position(19);
        ByteBuffer b4 = BufferUtils.ensureCapacity(b, b.position() + 20);
        BufferUtils.flipToFill(b4);
        b4.position(b.position());

        assertEquals(20, b4.remaining());
        assertEquals(19, b4.position());
        assertEquals(39, b4.capacity());

        BufferUtils.flipToFill(b);
        ByteBuffer b5 = BufferUtils.allocateDirect(19);
        assertEquals(0, b5.remaining());
        assertEquals(19, b.remaining());

        BufferUtils.append(b5, b);
        assertEquals(0, b.remaining());
        assertEquals(19, b5.remaining());
        BufferUtils.flipToFill(b);
        assertEquals(19, b.remaining());

        ByteBuffer b6 = BufferUtils.ensureCapacity(b5, 20);
        assertEquals(19, b6.remaining());
        BufferUtils.flipToFill(b6);
        assertEquals(1, b6.remaining());
    }

    @Test
    void testAddCapacity() {
        ByteBuffer b = BufferUtils.toBuffer("Goodbye Cruel World");
        b.position(19);
        assertEquals(0, b.remaining());
        ByteBuffer b1 = BufferUtils.addCapacity(b, 20);
        assertEquals(b1.remaining(), 20);
        assertEquals(19, b1.position());
        assertEquals(39, b1.capacity());
        assertEquals(39, b1.limit());
    }

    @Test
    void testToDetail_WithDEL() {
        ByteBuffer b = ByteBuffer.allocate(40);
        b.putChar('a').putChar('b').putChar('c');
        b.put((byte) 0x7F);
        b.putChar('x').putChar('y').putChar('z');
        b.flip();
        String result = BufferUtils.toDetailString(b);
        assertTrue(result.contains("\\x7f"));
    }


    private void testWriteToWithBufferThatDoesNotExposeArray(int capacity) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = new byte[capacity];
        ThreadLocalRandom.current().nextBytes(bytes);
        ByteBuffer buffer = BufferUtils.allocate(capacity);
        BufferUtils.append(buffer, bytes, 0, capacity);
        BufferUtils.writeTo(buffer.asReadOnlyBuffer(), out);
        assertArrayEquals(bytes, out.toByteArray());
    }
}
