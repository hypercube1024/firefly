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
    }

    public static void debug(Class<?> clazz, Throwable t, Supplier<String> supplier) {
        if (system.isDebugEnabled()) {
            system.debug("[" + clazz.getName() + "] " + supplier.get(), t);
        }
    }
}
