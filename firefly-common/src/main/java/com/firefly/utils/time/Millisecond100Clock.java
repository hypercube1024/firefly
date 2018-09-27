package com.firefly.utils.time;

public class Millisecond100Clock {
    private static final TimeProvider TIME_PROVIDER = new TimeProvider(100);

    static {
        TIME_PROVIDER.start();
    }

    public static long currentTimeMillis() {
        return TIME_PROVIDER.currentTimeMillis();
    }

    public static void stop() {
        TIME_PROVIDER.stop();
    }
}
