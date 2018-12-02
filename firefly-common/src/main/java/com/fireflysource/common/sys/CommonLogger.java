package com.fireflysource.common.sys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * @author Pengtao Qiu
 */
abstract public class CommonLogger {
    public static final Logger system = LoggerFactory.getLogger("firefly-system");

    public static void debug(Class<?> clazz, Supplier<String> supplier) {
        if (system.isDebugEnabled()) {
            system.debug("[" + clazz.getName() + "] " + supplier.get());
        }
//        System.out.println("[" + clazz.getName() + "] " + supplier.get());
    }

    public static void debug(Class<?> clazz, Throwable t, Supplier<String> supplier) {
        if (system.isDebugEnabled()) {
            system.debug("[" + clazz.getName() + "] " + supplier.get(), t);
        }
    }

    public static void info(Class<?> clazz, Supplier<String> supplier) {
        if (system.isInfoEnabled()) {
            system.info("[" + clazz.getName() + "] " + supplier.get());
        }
    }

    public static void info(Class<?> clazz, Throwable t, Supplier<String> supplier) {
        if (system.isInfoEnabled()) {
            system.info("[" + clazz.getName() + "] " + supplier.get(), t);
        }
    }

    public static void warn(Class<?> clazz, Supplier<String> supplier) {
        if (system.isWarnEnabled()) {
            system.warn("[" + clazz.getName() + "] " + supplier.get());
        }
    }

    public static void warn(Class<?> clazz, Throwable t, Supplier<String> supplier) {
        if (system.isWarnEnabled()) {
            system.warn("[" + clazz.getName() + "] " + supplier.get(), t);
        }
    }

    public static void error(Class<?> clazz, Supplier<String> supplier) {
        if (system.isErrorEnabled()) {
            system.error("[" + clazz.getName() + "] " + supplier.get());
        }
    }

    public static void error(Class<?> clazz, Throwable t, Supplier<String> supplier) {
        if (system.isErrorEnabled()) {
            system.error("[" + clazz.getName() + "] " + supplier.get(), t);
        }
    }
}
