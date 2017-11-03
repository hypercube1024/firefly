package com.firefly.codec.http2.stream;

import com.firefly.utils.time.Millisecond100Clock;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Pengtao Qiu
 */
abstract public class ShutdownHelper {

    public static void destroy() {
        try {
            ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
            try {
                iLoggerFactory.getClass().getDeclaredMethod("stop").invoke(iLoggerFactory);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                System.out.println(e.getMessage());
            }
            Millisecond100Clock.stop();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
