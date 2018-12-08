package com.fireflysource.net.http.v2.hpack;


import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.object.TypeUtils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;


class TestNBitInteger {

    @Test
    void testOctetsNeeded() {
        assertEquals(0, NBitInteger.octectsNeeded(5, 10));
        assertEquals(2, NBitInteger.octectsNeeded(5, 1337));
        assertEquals(1, NBitInteger.octectsNeeded(8, 42));
        assertEquals(3, NBitInteger.octectsNeeded(8, 1337));

        assertEquals(0, NBitInteger.octectsNeeded(6, 62));
        assertEquals(1, NBitInteger.octectsNeeded(6, 63));
        assertEquals(1, NBitInteger.octectsNeeded(6, 64));
        assertEquals(2, NBitInteger.octectsNeeded(6, 63 + 0x00 + 0x80 * 0x01));
        assertEquals(3, NBitInteger.octectsNeeded(6, 63 + 0x00 + 0x80 * 0x80));
        assertEquals(4, NBitInteger.octectsNeeded(6, 63 + 0x00 + 0x80 * 0x80 * 0x80));
    }

    @Test
    void testEncode() {
        testEncode(6, 0, "00");
        testEncode(6, 1, "01");
        testEncode(6, 62, "3e");
        testEncode(6, 63, "3f00");
        testEncode(6, 63 + 1, "3f01");
        testEncode(6, 63 + 0x7e, "3f7e");
        testEncode(6, 63 + 0x7f, "3f7f");
        testEncode(6, 63 + 0x00 + 0x80 * 0x01, "3f8001");
        testEncode(6, 63 + 0x01 + 0x80 * 0x01, "3f8101");
        testEncode(6, 63 + 0x7f + 0x80 * 0x01, "3fFf01");
        testEncode(6, 63 + 0x00 + 0x80 * 0x02, "3f8002");
        testEncode(6, 63 + 0x01 + 0x80 * 0x02, "3f8102");
        testEncode(6, 63 + 0x7f + 0x80 * 0x7f, "3fFf7f");
        testEncode(6, 63 + 0x00 + 0x80 * 0x80, "3f808001");
        testEncode(6, 63 + 0x7f + 0x80 * 0x80 * 0x7f, "3fFf807f");
        testEncode(6, 63 + 0x00 + 0x80 * 0x80 * 0x80, "3f80808001");

        testEncode(8, 0, "00");
        testEncode(8, 1, "01");
        testEncode(8, 128, "80");
        testEncode(8, 254, "Fe");
        testEncode(8, 255, "Ff00");
        testEncode(8, 255 + 1, "Ff01");
        testEncode(8, 255 + 0x7e, "Ff7e");
        testEncode(8, 255 + 0x7f, "Ff7f");
        testEncode(8, 255 + 0x80, "Ff8001");
        testEncode(8, 255 + 0x00 + 0x80 * 0x80, "Ff808001");
    }

    void testEncode(int n, int i, String expected) {
        ByteBuffer buf = ByteBuffer.allocate(16);
        if (n < 8)
            buf.put((byte) 0x00);
        NBitInteger.encode(buf, n, i);
        buf.flip();
        String r = TypeUtils.toHexString(BufferUtils.toArray(buf));
        assertEquals(expected, r);

        assertEquals(expected.length() / 2, (n < 8 ? 1 : 0) + NBitInteger.octectsNeeded(n, i));
    }

    @Test
    void testDecode() {
        testDecode(6, 0, "00");
        testDecode(6, 1, "01");
        testDecode(6, 62, "3e");
        testDecode(6, 63, "3f00");
        testDecode(6, 63 + 1, "3f01");
        testDecode(6, 63 + 0x7e, "3f7e");
        testDecode(6, 63 + 0x7f, "3f7f");
        testDecode(6, 63 + 0x80, "3f8001");
        testDecode(6, 63 + 0x81, "3f8101");
        testDecode(6, 63 + 0x7f + 0x80 * 0x01, "3fFf01");
        testDecode(6, 63 + 0x00 + 0x80 * 0x02, "3f8002");
        testDecode(6, 63 + 0x01 + 0x80 * 0x02, "3f8102");
        testDecode(6, 63 + 0x7f + 0x80 * 0x7f, "3fFf7f");
        testDecode(6, 63 + 0x00 + 0x80 * 0x80, "3f808001");
        testDecode(6, 63 + 0x7f + 0x80 * 0x80 * 0x7f, "3fFf807f");
        testDecode(6, 63 + 0x00 + 0x80 * 0x80 * 0x80, "3f80808001");

        testDecode(8, 0, "00");
        testDecode(8, 1, "01");
        testDecode(8, 128, "80");
        testDecode(8, 254, "Fe");
        testDecode(8, 255, "Ff00");
        testDecode(8, 255 + 1, "Ff01");
        testDecode(8, 255 + 0x7e, "Ff7e");
        testDecode(8, 255 + 0x7f, "Ff7f");
        testDecode(8, 255 + 0x80, "Ff8001");
        testDecode(8, 255 + 0x00 + 0x80 * 0x80, "Ff808001");
    }

    void testDecode(int n, int expected, String encoded) {
        ByteBuffer buf = ByteBuffer.wrap(TypeUtils.fromHexString(encoded));
        buf.position(n == 8 ? 0 : 1);
        assertEquals(expected, NBitInteger.decode(buf, n));
    }

    @Test
    void testEncodeExampleD_1_1() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.put((byte) 0x77);
        buf.put((byte) 0xFF);
        NBitInteger.encode(buf, 5, 10);
        buf.flip();

        String r = TypeUtils.toHexString(BufferUtils.toArray(buf));

        assertEquals("77Ea", r);

    }

    @Test
    void testDecodeExampleD_1_1() {
        ByteBuffer buf = ByteBuffer.wrap(TypeUtils.fromHexString("77EaFF"));
        buf.position(2);

        assertEquals(10, NBitInteger.decode(buf, 5));
    }

    @Test
    void testEncodeExampleD_1_2() {
        ByteBuffer buf = ByteBuffer.allocate(16);

        buf.put((byte) 0x88);
        buf.put((byte) 0x00);
        NBitInteger.encode(buf, 5, 1337);
        buf.flip();

        String r = TypeUtils.toHexString(BufferUtils.toArray(buf));

        assertEquals("881f9a0a", r);

    }

    @Test
    void testDecodeExampleD_1_2() {
        ByteBuffer buf = ByteBuffer.wrap(TypeUtils.fromHexString("881f9a0aff"));
        buf.position(2);

        assertEquals(1337, NBitInteger.decode(buf, 5));
    }

    @Test
    void testEncodeExampleD_1_3() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.put((byte) 0x88);
        buf.put((byte) 0xFF);
        NBitInteger.encode(buf, 8, 42);
        buf.flip();

        String r = TypeUtils.toHexString(BufferUtils.toArray(buf));

        assertEquals("88Ff2a", r);

    }

    @Test
    void testDecodeExampleD_1_3() {
        ByteBuffer buf = ByteBuffer.wrap(TypeUtils.fromHexString("882aFf"));
        buf.position(1);

        assertEquals(42, NBitInteger.decode(buf, 8));
    }

}
