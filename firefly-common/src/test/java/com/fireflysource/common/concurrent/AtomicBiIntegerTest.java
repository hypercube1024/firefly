package com.fireflysource.common.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AtomicBiIntegerTest {

    @Test
    void testBitOperations() {
        long encoded;

        encoded = AtomicBiInteger.encode(0, 0);
        assertEquals(0, AtomicBiInteger.getHi(encoded));
        assertEquals(0, AtomicBiInteger.getLo(encoded));

        encoded = AtomicBiInteger.encode(1, 2);
        assertEquals(1, AtomicBiInteger.getHi(encoded));
        assertEquals(2, AtomicBiInteger.getLo(encoded));

        encoded = AtomicBiInteger.encode(Integer.MAX_VALUE, -1);
        assertEquals(Integer.MAX_VALUE, AtomicBiInteger.getHi(encoded));
        assertEquals(-1, AtomicBiInteger.getLo(encoded));
        encoded = AtomicBiInteger.encodeLo(encoded, 42);
        assertEquals(Integer.MAX_VALUE, AtomicBiInteger.getHi(encoded));
        assertEquals(42, AtomicBiInteger.getLo(encoded));

        encoded = AtomicBiInteger.encode(-1, Integer.MAX_VALUE);
        assertEquals(-1, AtomicBiInteger.getHi(encoded));
        assertEquals(Integer.MAX_VALUE, AtomicBiInteger.getLo(encoded));
        encoded = AtomicBiInteger.encodeHi(encoded, 42);
        assertEquals(42, AtomicBiInteger.getHi(encoded));
        assertEquals(Integer.MAX_VALUE, AtomicBiInteger.getLo(encoded));

        encoded = AtomicBiInteger.encode(Integer.MIN_VALUE, 1);
        assertEquals(Integer.MIN_VALUE, AtomicBiInteger.getHi(encoded));
        assertEquals(1, AtomicBiInteger.getLo(encoded));
        encoded = AtomicBiInteger.encodeLo(encoded, Integer.MAX_VALUE);
        assertEquals(Integer.MIN_VALUE, AtomicBiInteger.getHi(encoded));
        assertEquals(Integer.MAX_VALUE, AtomicBiInteger.getLo(encoded));

        encoded = AtomicBiInteger.encode(1, Integer.MIN_VALUE);
        assertEquals(1, AtomicBiInteger.getHi(encoded));
        assertEquals(Integer.MIN_VALUE, AtomicBiInteger.getLo(encoded));
        encoded = AtomicBiInteger.encodeHi(encoded, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, AtomicBiInteger.getHi(encoded));
        assertEquals(Integer.MIN_VALUE, AtomicBiInteger.getLo(encoded));
    }

    @Test
    void testSet() {
        AtomicBiInteger abi = new AtomicBiInteger();
        assertEquals(0, abi.getHi());
        assertEquals(0, abi.getLo());

        abi.getAndSetHi(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, abi.getHi());
        assertEquals(0, abi.getLo());

        abi.getAndSetLo(Integer.MIN_VALUE);
        assertEquals(Integer.MAX_VALUE, abi.getHi());
        assertEquals(Integer.MIN_VALUE, abi.getLo());
    }

    @Test
    void testCompareAndSet() {
        AtomicBiInteger abi = new AtomicBiInteger();
        assertEquals(0, abi.getHi());
        assertEquals(0, abi.getLo());

        assertFalse(abi.compareAndSetHi(1, 42));
        assertTrue(abi.compareAndSetHi(0, 42));
        assertEquals(42, abi.getHi());
        assertEquals(0, abi.getLo());

        assertFalse(abi.compareAndSetLo(1, -42));
        assertTrue(abi.compareAndSetLo(0, -42));
        assertEquals(42, abi.getHi());
        assertEquals(-42, abi.getLo());
    }
}
