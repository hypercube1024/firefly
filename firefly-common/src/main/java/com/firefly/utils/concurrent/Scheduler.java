package com.firefly.utils.concurrent;

import com.firefly.utils.lang.LifeCycle;

import java.util.concurrent.TimeUnit;

public interface Scheduler extends LifeCycle {

    interface Future {
        boolean cancel();
    }

    Future schedule(Runnable task, long delay, TimeUnit unit);

    Future scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit);

    Future scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit);
}
