package com.fireflysource.log.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * @author Pengtao Qiu
 */
public class LazyLogger {

    private final Logger logger;

    public LazyLogger(Logger logger) {
        this.logger = logger;
    }

    public void trace(Supplier<String> supplier) {
        if (logger.isTraceEnabled()) {
            logger.trace(supplier.get());
        }
    }

    public void trace(Supplier<String> supplier, Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.trace(supplier.get(), t);
        }
    }

    public void debug(Supplier<String> supplier) {
        if (logger.isDebugEnabled()) {
            logger.debug(supplier.get());
        }
    }

    public void debug(Supplier<String> supplier, Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.debug(supplier.get(), t);
        }
    }

    public void info(Supplier<String> supplier) {
        if (logger.isInfoEnabled()) {
            logger.info(supplier.get());
        }
    }

    public void info(Supplier<String> supplier, Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.info(supplier.get(), t);
        }
    }

    public void warn(Supplier<String> supplier) {
        if (logger.isWarnEnabled()) {
            logger.warn(supplier.get());
        }
    }

    public void warn(Supplier<String> supplier, Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.warn(supplier.get(), t);
        }
    }

    public void error(Supplier<String> supplier) {
        if (logger.isErrorEnabled()) {
            logger.error(supplier.get());
        }
    }

    public void error(Supplier<String> supplier, Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.error(supplier.get(), t);
        }
    }

    public static LazyLogger create() {
        StackTraceElement[] arr = Thread.currentThread().getStackTrace();
        return new LazyLogger(LoggerFactory.getLogger(arr[2].getClassName()));
    }

    public static LazyLogger create(String name) {
        return new LazyLogger(LoggerFactory.getLogger(name));
    }

    public static LazyLogger create(Class<?> clazz) {
        return new LazyLogger(LoggerFactory.getLogger(clazz));
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public Logger getLogger() {
        return logger;
    }
}
