package com.firefly.utils.log;

/**
 * @author Pengtao Qiu
 */
public class DefaultLogFormatter implements LogFormatter {
    @Override
    public String format(LogItem logItem) {
        return logItem.toString();
    }
}
