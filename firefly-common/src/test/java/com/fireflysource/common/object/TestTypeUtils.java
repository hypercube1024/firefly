package com.fireflysource.common.object;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestTypeUtils {
    @Test
    void convertHexDigitTest() {
        assertEquals((byte) 0, TypeUtils.convertHexDigit((byte) '0'));
        assertEquals((byte) 9, TypeUtils.convertHexDigit((byte) '9'));
        assertEquals((byte) 10, TypeUtils.convertHexDigit((byte) 'a'));
        assertEquals((byte) 10, TypeUtils.convertHexDigit((byte) 'A'));
        assertEquals((byte) 15, TypeUtils.convertHexDigit((byte) 'f'));
        assertEquals((byte) 15, TypeUtils.convertHexDigit((byte) 'F'));

        assertEquals(0, TypeUtils.convertHexDigit((int) '0'));
        assertEquals(9, TypeUtils.convertHexDigit((int) '9'));
        assertEquals(10, TypeUtils.convertHexDigit((int) 'a'));
        assertEquals(10, TypeUtils.convertHexDigit((int) 'A'));
        assertEquals(15, TypeUtils.convertHexDigit((int) 'f'));
        assertEquals(15, TypeUtils.convertHexDigit((int) 'F'));
    }

    @Test
    void testToHexInt() throws Exception {
        StringBuilder b = new StringBuilder();

        b.setLength(0);
        TypeUtils.toHex(0, b);
        assertEquals("00000000", b.toString());

        b.setLength(0);
        TypeUtils.toHex(Integer.MAX_VALUE, b);
        assertEquals("7FFFFFFF", b.toString());

        b.setLength(0);
        TypeUtils.toHex(Integer.MIN_VALUE, b);
        assertEquals("80000000", b.toString());

        b.setLength(0);
        TypeUtils.toHex(0x12345678, b);
        assertEquals("12345678", b.toString());

        b.setLength(0);
        TypeUtils.toHex(0x9abcdef0, b);
        assertEquals("9ABCDEF0", b.toString());
    }

    @Test
    void testToHexLong() throws Exception {
        StringBuilder b = new StringBuilder();

        b.setLength(0);
        TypeUtils.toHex((long) 0, b);
        assertEquals("0000000000000000", b.toString());

        b.setLength(0);
        TypeUtils.toHex(Long.MAX_VALUE, b);
        assertEquals("7FFFFFFFFFFFFFFF", b.toString());

        b.setLength(0);
        TypeUtils.toHex(Long.MIN_VALUE, b);
        assertEquals("8000000000000000", b.toString());

        b.setLength(0);
        TypeUtils.toHex(0x123456789abcdef0L, b);
        assertEquals("123456789ABCDEF0", b.toString());
    }

    @Test
    void testIsTrue() {
        assertTrue(TypeUtils.isTrue(Boolean.TRUE));
        assertTrue(TypeUtils.isTrue(true));
        assertTrue(TypeUtils.isTrue("true"));
        assertTrue(TypeUtils.isTrue(new Object() {
            @Override
            public String toString() {
                return "true";
            }
        }));

        assertFalse(TypeUtils.isTrue(Boolean.FALSE));
        assertFalse(TypeUtils.isTrue(false));
        assertFalse(TypeUtils.isTrue("false"));
        assertFalse(TypeUtils.isTrue("blargle"));
        assertFalse(TypeUtils.isTrue(new Object() {
            @Override
            public String toString() {
                return "false";
            }
        }));
    }

    @Test
    void testIsFalse() {
        assertTrue(TypeUtils.isFalse(Boolean.FALSE));
        assertTrue(TypeUtils.isFalse(false));
        assertTrue(TypeUtils.isFalse("false"));
        assertTrue(TypeUtils.isFalse(new Object() {
            @Override
            public String toString() {
                return "false";
            }
        }));

        assertFalse(TypeUtils.isFalse(Boolean.TRUE));
        assertFalse(TypeUtils.isFalse(true));
        assertFalse(TypeUtils.isFalse("true"));
        assertFalse(TypeUtils.isFalse("blargle"));
        assertFalse(TypeUtils.isFalse(new Object() {
            @Override
            public String toString() {
                return "true";
            }
        }));
    }
}
