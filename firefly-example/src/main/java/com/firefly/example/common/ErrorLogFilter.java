package com.firefly.example.common;

import com.firefly.utils.log.LogFilter;
import com.firefly.utils.log.LogItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengtao Qiu
 */
public class ErrorLogFilter implements LogFilter {

    private static Logger logger = LoggerFactory.getLogger("firefly-example-error");

    @Override
    public void filter(LogItem logItem) {
        if (logItem.getLevel().equals("ERROR")) {
            logger.error(logItem.getContent(), logItem.getThrowable());
        }
    }
}
