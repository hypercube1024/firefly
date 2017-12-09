package com.firefly.utils.retry;

import com.firefly.utils.Assert;
import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.function.Action1;

import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
abstract public class WaitStrategies {
    public static <V> Action1<TaskContext<V>> fixedTime(long sleepTime, TimeUnit timeUnit) {
        return ctx -> ThreadUtils.sleep(sleepTime, timeUnit);
    }

    public static <V> Action1<TaskContext<V>> exponentialIncrease(long initTime, long maxTime, TimeUnit timeUnit,
                                                                  int multiply) {
        Assert.isTrue(multiply > 0, "The multiply must be great than 0");
        Assert.isTrue(maxTime >= initTime, "The max time must be great than or equals init time");
        return ctx -> {
           long timeout = initTime << ctx.getExecutedCount() * multiply;
        };
    }

}
