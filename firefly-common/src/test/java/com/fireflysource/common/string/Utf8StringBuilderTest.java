package com.fireflysource.common.string;

import com.fireflysource.common.object.TypeUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;


class Utf8StringBuilderTest {

    @Test
    void testFastFail_1() {
        byte[] part1 = TypeUtils.fromHexString("cebae1bdb9cf83cebcceb5");
        byte[] part2 = TypeUtils.fromHexString("f4908080"); // INVALID
        // Here for test tracking reasons, not needed to satisfy test
        // byte[] part3 = TypeUtil.fromHexString("656469746564");

        Utf8StringBuilder buffer = new Utf8StringBuilder();
        // Part 1 is valid
        buffer.append(part1, 0, part1.length);
        try {
            // Part 2 is invalid
            buffer.append(part2, 0, part2.length);
            fail("Should have thrown a NotUtf8Exception");
        } catch (Utf8Appendable.NotUtf8Exception e) {
            // expected path
        }
    }

    @Test
    void testFastFail_2() {
        byte[] part1 = TypeUtils.fromHexString("cebae1bdb9cf83cebcceb5f4");
        byte[] part2 = TypeUtils.fromHexString("90"); // INVALID
        // Here for test search/tracking reasons, not needed to satisfy test
        // byte[] part3 = TypeUtil.fromHexString("8080656469746564");

        Utf8StringBuilder buffer = new Utf8StringBuilder();
        // Part 1 is valid
        buffer.append(part1, 0, part1.length);
        try {
            // Part 2 is invalid
            buffer.append(part2, 0, part2.length);
            fail("Should have thrown a NotUtf8Exception");
        } catch (Utf8Appendable.NotUtf8Exception e) {
            // expected path
        }
    }

    @Test
    void testUtfStringBuilder() {
        String source = "abcd012345\n\r\u0000\u00a4\u10fb\ufffdfirefly";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        Utf8StringBuilder buffer = new Utf8StringBuilder();
        for (byte aByte : bytes)
            buffer.append(aByte);
        assertEquals(source, buffer.toString());
        assertTrue(buffer.toString().endsWith("firefly"));
    }

    @Test
    void testShort() {
        assertThrows(IllegalArgumentException.class, () -> {
            String source = "abc\u10fb";
            byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
            Utf8StringBuilder buffer = new Utf8StringBuilder();
            for (int i = 0; i < bytes.length - 1; i++) {
                buffer.append(bytes[i]);
            }
            buffer.toString();
        });
    }

    @Test
    void testLong() {
        String source = "abcXX";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        bytes[3] = (byte) 0xc0;
        bytes[4] = (byte) 0x00;

        Utf8StringBuilder buffer = new Utf8StringBuilder();
        try {
            for (byte aByte : bytes) {
                buffer.append(aByte);
            }
            fail("Should have resulted in an Utf8Appendable.NotUtf8Exception");
        } catch (Utf8Appendable.NotUtf8Exception e) {
            // expected path
        }
        assertEquals("abc\ufffd", buffer.toString());
    }

    @Test
    void testUTF32codes() {
        String source = "\uD842\uDF9F";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);

        String jvmcheck = new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
        assertEquals(source, jvmcheck);

        Utf8StringBuilder buffer = new Utf8StringBuilder();
        buffer.append(bytes, 0, bytes.length);
        String result = buffer.toString();
        assertEquals(source, result);
    }
}
