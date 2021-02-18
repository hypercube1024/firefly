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

    public static boolean updateMin(AtomicInteger currentMin, int newValue) {
        int oldValue = currentMin.get();
        while (newValue < oldValue) {
            if (currentMin.compareAndSet(oldValue, newValue)) {
                return true;
            }
            oldValue = currentMin.get();
        }
        return false;
    }

    public static boolean updateMax(AtomicInteger currentMax, int newValue) {
        int oldValue = currentMax.get();
        while (newValue > oldValue) {
            if (currentMax.compareAndSet(oldValue, newValue)) {
                return true;
            }
            oldValue = currentMax.get();
        }
        return false;
    }
}
