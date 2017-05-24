package com.firefly.utils.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Atomics {
    private Atomics() {
    }

    public static boolean updateMin(AtomicLong currentMin, long newValue) {
        long oldValue = currentMin.get();
        while (newValue < oldValue) {
            if (currentMin.compareAndSet(oldValue, newValue))
                return true;
            oldValue = currentMin.get();
        }
        return false;
    }

    public static boolean updateMax(AtomicLong currentMax, long newValue) {
        long oldValue = currentMax.get();
        while (newValue > oldValue) {
            if (currentMax.compareAndSet(oldValue, newValue))
                return true;
            oldValue = currentMax.get();
        }
        return false;
    }

    public static boolean updateMin(AtomicInteger currentMin, int newValue) {
        int oldValue = currentMin.get();
        while (newValue < oldValue) {
            if (currentMin.compareAndSet(oldValue, newValue))
                return true;
            oldValue = currentMin.get();
        }
        return false;
    }

    public static boolean updateMax(AtomicInteger currentMax, int newValue) {
        int oldValue = currentMax.get();
        while (newValue > oldValue) {
            if (currentMax.compareAndSet(oldValue, newValue))
                return true;
            oldValue = currentMax.get();
        }
        return false;
    }

    public static int getAndDecrement(AtomicInteger i, int minValue) {
        int p = i.getAndUpdate(prev -> {
            if (prev > minValue) {
                return prev - 1;
            } else {
                return minValue;
            }
        });
        return p;
    }

    public static int getAndIncrement(AtomicInteger i, int maxValue) {
        int p = i.getAndUpdate(prev -> {
            if (prev < maxValue) {
                return prev + 1;
            } else {
                return maxValue;
            }
        });
        return p;
    }
}
