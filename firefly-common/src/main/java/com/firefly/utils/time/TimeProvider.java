package com.firefly.utils.time;

import com.firefly.utils.lang.AbstractLifeCycle;

public class TimeProvider extends AbstractLifeCycle {

    private final long interval;
    private volatile long current = System.currentTimeMillis();

    public TimeProvider(long interval) {
        this.interval = interval;
    }

    public long currentTimeMillis() {
        return current;
    }

    @Override
    protected void init() {
        start = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (start) {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        System.err.println("sleep exception, " + e.getMessage());
                    }
                    current = System.currentTimeMillis();
                }
            }
        }, "filefly time provider " + interval + "ms").start();
    }

    @Override
    protected void destroy() {
        start = false;
    }

}
