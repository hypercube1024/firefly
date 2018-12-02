package com.fireflysource.log;

/**
 * @author Pengtao Qiu
 */
@FunctionalInterface
public interface LogFilter {

    void filter(LogItem logItem);

}
