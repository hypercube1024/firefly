package com.fireflysource.log;

/**
 * @author Pengtao Qiu
 */
@FunctionalInterface
public interface LogFormatter {

    String format(LogItem logItem);

}
