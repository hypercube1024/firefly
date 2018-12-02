package com.fireflysource.log;

/**
 * @author Pengtao Qiu
 */
public class ClassNameLogWrap implements Log {

    public static final ThreadLocal<String> name = new ThreadLocal<>();

    private final Log log;
    private final String className;

    public ClassNameLogWrap(Log log, String className) {
        this.log = log;
        this.className = className;
    }

    public String getName() {
        return log.getName();
    }

    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    public void trace(String str) {
        name.set(className);
        log.trace(str);
    }

    public void trace(String str, Object... objs) {
        name.set(className);
        log.trace(str, objs);
    }

    public void trace(String str, Throwable throwable, Object... objs) {
        name.set(className);
        log.trace(str, throwable, objs);
    }

    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    public void debug(String str) {
        name.set(className);
        log.debug(str);
    }

    public void debug(String str, Object... objs) {
        name.set(className);
        log.debug(str, objs);
    }

    public void debug(String str, Throwable throwable, Object... objs) {
        name.set(className);
        log.debug(str, throwable, objs);
    }

    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    public void info(String str) {
        name.set(className);
        log.info(str);
    }

    public void info(String str, Object... objs) {
        name.set(className);
        log.info(str, objs);
    }

    public void info(String str, Throwable throwable, Object... objs) {
        name.set(className);
        log.info(str, throwable, objs);
    }

    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    public void warn(String str) {
        name.set(className);
        log.warn(str);
    }

    public void warn(String str, Object... objs) {
        name.set(className);
        log.warn(str, objs);
    }

    public void warn(String str, Throwable throwable, Object... objs) {
        name.set(className);
        log.warn(str, throwable, objs);
    }

    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    public void error(String str) {
        name.set(className);
        log.error(str);
    }

    public void error(String str, Object... objs) {
        name.set(className);
        log.error(str, objs);
    }

    public void error(String str, Throwable throwable, Object... objs) {
        name.set(className);
        log.error(str, throwable, objs);
    }

    public Log getLog() {
        return log;
    }

    public String getClassName() {
        return className;
    }
}
