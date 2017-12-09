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

    public static <V> Action1<TaskContext<V>> multipleWait(long initTime, long maxTime, TimeUnit timeUnit, int multiple) {
        Assert.isTrue(multiple > 0, "The multiple must be great than 0");
        Assert.isTrue(maxTime >= initTime, "The max time must be great than or equals init time");
        return ctx -> {
            int count = Math.max(ctx.getExecutedCount(), 1) - 1;
            long sleepTime = initTime << (count * (multiple - 1));
            ThreadUtils.sleep(Math.min(sleepTime, maxTime), timeUnit);
        };
    }

    public static <V> Action1<TaskContext<V>> multipleWait(long initTime, long maxTime, TimeUnit timeUnit) {
        return multipleWait(initTime, maxTime, timeUnit, 2);
    }

    public static <V> Action1<TaskContext<V>> multipleWait(long initTime, TimeUnit timeUnit) {
        return multipleWait(initTime, Long.MAX_VALUE, timeUnit, 2);
    }

    public static void main(String[] args) {
        for (int i = 1; i < 5; i++) {
            System.out.println(3 << ((i - 1) * (3 - 1)));
        }
    }

}
