package com.firefly.utils.retry;

import com.firefly.utils.Assert;
import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.function.Action1;

import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
abstract public class WaitStrategies {
    public static <V> Action1<TaskContext<V>> fixedWait(long sleepTime, TimeUnit timeUnit) {
        return ctx -> ThreadUtils.sleep(sleepTime, timeUnit);
    }

    /**
     * The exponential increase sleep time. The formula: initTime * 2 ^ ((count - 1) * (multiple - 1)).
     *
     * @param initTime The first sleep time.
     * @param maxTime  The max sleep time.
     * @param timeUnit The time unit.
     * @param multiple The growth factor.
     * @param <V>      The return value type.
     * @return The task result.
     */
    public static <V> Action1<TaskContext<V>> exponentialWait(long initTime, long maxTime, TimeUnit timeUnit, int multiple) {
        Assert.isTrue(multiple > 0, "The multiple must be great than 0");
        Assert.isTrue(maxTime >= initTime, "The max time must be great than or equals init time");
        return ctx -> {
            int count = Math.max(ctx.getExecutedCount(), 1) - 1;
            long sleepTime = initTime << (count * (multiple - 1));
            ThreadUtils.sleep(Math.min(sleepTime, maxTime), timeUnit);
        };
    }

    public static <V> Action1<TaskContext<V>> exponentialWait(long initTime, long maxTime, TimeUnit timeUnit) {
        return exponentialWait(initTime, maxTime, timeUnit, 2);
    }

    public static <V> Action1<TaskContext<V>> exponentialWait(long initTime, TimeUnit timeUnit) {
        return exponentialWait(initTime, Long.MAX_VALUE, timeUnit, 2);
    }

    public static void main(String[] args) {
        for (int i = 1; i < 5; i++) {
            System.out.println(3 << ((i - 1) * (3 - 1)));
            System.out.println(3 << (i - 1));
            System.out.println("------------");
        }
    }

}
