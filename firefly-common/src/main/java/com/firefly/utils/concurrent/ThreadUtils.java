package com.firefly.utils.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
abstract public class ThreadUtils {

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public static void sleep(long timeout, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(timeout);
        } catch (InterruptedException ignored) {
        }
    }
}
