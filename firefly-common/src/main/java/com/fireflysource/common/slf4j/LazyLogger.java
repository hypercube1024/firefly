package com.fireflysource.common.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MarkerIgnoringBase;

import java.util.function.Supplier;

/**
 * @author Pengtao Qiu
 */
public class LazyLogger extends MarkerIgnoringBase {

    private Logger logger;

    public LazyLogger() {
    }

    public LazyLogger(Logger logger) {
        this.logger = logger;
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

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void trace(Supplier<String> supplier) {
        if (logger.isTraceEnabled()) {
            logger.trace(supplier.get());
        }
    }

    public void trace(Throwable t, Supplier<String> supplier) {
        if (logger.isTraceEnabled()) {
            logger.trace(supplier.get(), t);
        }
    }

    public void debug(Supplier<String> supplier) {
        if (logger.isDebugEnabled()) {
            logger.debug(supplier.get());
        }
    }

    public void debug(Throwable t, Supplier<String> supplier) {
        if (logger.isDebugEnabled()) {
            logger.debug(supplier.get(), t);
        }
    }

    public void info(Supplier<String> supplier) {
        if (logger.isInfoEnabled()) {
            logger.info(supplier.get());
        }
    }

    public void info(Throwable t, Supplier<String> supplier) {
        if (logger.isInfoEnabled()) {
            logger.info(supplier.get(), t);
        }
    }

    public void warn(Supplier<String> supplier) {
        if (logger.isWarnEnabled()) {
            logger.warn(supplier.get());
        }
    }

    public void warn(Throwable t, Supplier<String> supplier) {
        if (logger.isWarnEnabled()) {
            logger.warn(supplier.get(), t);
        }
    }

    public void error(Supplier<String> supplier) {
        if (logger.isErrorEnabled()) {
            logger.error(supplier.get());
        }
    }

    public void error(Throwable t, Supplier<String> supplier) {
        if (logger.isErrorEnabled()) {
            logger.error(supplier.get(), t);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logger.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logger.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(msg, t);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        logger.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logger.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(msg, t);
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        logger.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logger.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        logger.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }

}
