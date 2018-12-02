package com.fireflysource.common.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Pengtao Qiu
 */
class TestAtomics {

    @Test
    void testGetAndDecrement() {
        int init = 10;
        int min = 5;
        AtomicInteger integer = new AtomicInteger(init);

        for (int i = init; i > 0; i--) {
            Atomics.getAndDecrement(integer, min);
        }
        assertEquals(min, integer.get());
    }

    @Test
    void testGetAndIncrement() {
        int max = 10;
        AtomicInteger integer = new AtomicInteger(0);
        for (int i = 0; i < 20; i++) {
            Atomics.getAndIncrement(integer, max);
        }
        assertEquals(max, integer.get());
    }
}
