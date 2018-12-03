package com.fireflysource.common.sys;

import com.fireflysource.common.slf4j.LazyLogger;

import java.util.function.Supplier;

/**
 * @author Pengtao Qiu
 */
public class CommonLogger {
    private static final LazyLogger system = LazyLogger.create("firefly-system");

    private final Class<?> clazz;

    private CommonLogger(Class<?> clazz) {
        this.clazz = clazz;
    }

    public static CommonLogger create(Class<?> clazz) {
        return new CommonLogger(clazz);
    }

    public void debug(Supplier<String> supplier) {
        system.debug(() -> "[" + clazz.getName() + "] " + supplier.get());
    }

    public void debug(Throwable t, Supplier<String> supplier) {
        system.debug(() -> "[" + clazz.getName() + "] " + supplier.get(), t);
    }

    public void info(Supplier<String> supplier) {
        system.info(() -> "[" + clazz.getName() + "] " + supplier.get());
    }

    public void info(Throwable t, Supplier<String> supplier) {
        system.info(() -> "[" + clazz.getName() + "] " + supplier.get(), t);
    }

    public void warn(Supplier<String> supplier) {
        system.warn(() -> "[" + clazz.getName() + "] " + supplier.get());
    }

    public void warn(Throwable t, Supplier<String> supplier) {
        system.warn(() -> "[" + clazz.getName() + "] " + supplier.get(), t);
    }

    public void error(Supplier<String> supplier) {
        system.error(() -> "[" + clazz.getName() + "] " + supplier.get());
    }

    public void error(Throwable t, Supplier<String> supplier) {
        system.error(() -> "[" + clazz.getName() + "] " + supplier.get(), t);
    }
}
