package com.fireflysource.common.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

abstract public class Atomics {

    public static int getAndDecrement(AtomicInteger i, int minValue) {
        return i.getAndUpdate(prev -> {
            if (prev > minValue) {
                return prev - 1;
            } else {
                return minValue;
            }
        });
    }

    public static int getAndIncrement(AtomicInteger i, int maxValue) {
        return i.getAndUpdate(prev -> {
            if (prev < maxValue) {
                return prev + 1;
            } else {
                return maxValue;
            }
        });
    }
}
