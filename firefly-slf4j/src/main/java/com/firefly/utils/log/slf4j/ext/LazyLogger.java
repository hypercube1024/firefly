package com.firefly.utils.log.slf4j.ext;

import com.firefly.utils.function.Func0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengtao Qiu
 */
public class LazyLogger {

    private final Logger logger;

    public LazyLogger(Logger logger) {
        this.logger = logger;
    }

    public void trace(Func0<String> func0) {
        if (logger.isTraceEnabled()) {
            logger.trace(func0.call());
        }
    }

    public void trace(Func0<String> func0, Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.trace(func0.call(), t);
        }
    }

    public void debug(Func0<String> func0) {
        if (logger.isDebugEnabled()) {
            logger.debug(func0.call());
        }
    }

    public void debug(Func0<String> func0, Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.debug(func0.call(), t);
        }
    }

    public void info(Func0<String> func0) {
        if (logger.isInfoEnabled()) {
            logger.info(func0.call());
        }
    }

    public void info(Func0<String> func0, Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.info(func0.call(), t);
        }
    }

    public void warn(Func0<String> func0) {
        if (logger.isWarnEnabled()) {
            logger.warn(func0.call());
        }
    }

    public void warn(Func0<String> func0, Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.warn(func0.call(), t);
        }
    }

    public void error(Func0<String> func0) {
        if (logger.isErrorEnabled()) {
            logger.error(func0.call());
        }
    }

    public void error(Func0<String> func0, Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.error(func0.call(), t);
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
}
