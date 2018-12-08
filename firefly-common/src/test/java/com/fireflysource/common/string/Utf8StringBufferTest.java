package com.fireflysource.common.string;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;


class Utf8StringBufferTest {
    @Test
    void testUtfStringBuffer() {
        String source = "abcd012345\n\r\u0000\u00a4\u10fb\ufffdjetty";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        Utf8StringBuffer buffer = new Utf8StringBuffer();
        for (byte aByte : bytes)
            buffer.append(aByte);
        assertEquals(source, buffer.toString());
        assertTrue(buffer.toString().endsWith("jetty"));
    }

    @Test
    void testUtf8WithMissingByte() {
        assertThrows(IllegalArgumentException.class, () -> {
            String source = "abc\u10fb";
            byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
            Utf8StringBuffer buffer = new Utf8StringBuffer();
            for (int i = 0; i < bytes.length - 1; i++) {
                buffer.append(bytes[i]);
            }
            buffer.toString();
        });
    }

    @Test
    void testUtf8WithAdditionalByte() {
        assertThrows(Utf8Appendable.NotUtf8Exception.class, () -> {
            String source = "abcXX";
            byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
            bytes[3] = (byte) 0xc0;
            bytes[4] = (byte) 0x00;

            Utf8StringBuffer buffer = new Utf8StringBuffer();
            for (byte aByte : bytes) {
                buffer.append(aByte);
            }
        });
    }

    @Test
    void testUTF32codes() {
        String source = "\uD842\uDF9F";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);

        String jvmcheck = new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
        assertEquals(source, jvmcheck);

        Utf8StringBuffer buffer = new Utf8StringBuffer();
        buffer.append(bytes, 0, bytes.length);
        String result = buffer.toString();
        assertEquals(source, result);
    }

    @Test
    void testGermanUmlauts() {
        byte[] bytes = new byte[6];
        bytes[0] = (byte) 0xC3;
        bytes[1] = (byte) 0xBC;
        bytes[2] = (byte) 0xC3;
        bytes[3] = (byte) 0xB6;
        bytes[4] = (byte) 0xC3;
        bytes[5] = (byte) 0xA4;

        Utf8StringBuffer buffer = new Utf8StringBuffer();
        for (int i = 0; i < bytes.length; i++)
            buffer.append(bytes[i]);

        assertEquals("\u00FC\u00F6\u00E4", buffer.toString());
    }

    @Test
    void testInvalidUTF8() {
        assertThrows(Utf8Appendable.NotUtf8Exception.class, () -> {
            Utf8StringBuffer buffer = new Utf8StringBuffer();
            buffer.append((byte) 0xC2);
            buffer.append((byte) 0xC2);
        });
    }
}
