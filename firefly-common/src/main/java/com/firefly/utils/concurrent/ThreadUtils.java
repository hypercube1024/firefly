package com.firefly.utils.concurrent;

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
}
