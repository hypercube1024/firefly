package com.firefly.boot.common;

import com.firefly.$;
import com.firefly.utils.CollectionUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.log.LogFormatter;
import com.firefly.utils.log.LogItem;
import com.firefly.utils.time.SafeSimpleDateFormat;

/**
 * @author Pengtao Qiu
 */
public class BootLogFormatter implements LogFormatter {

    @Override
    public String format(LogItem logItem) {
        String logStr = logItem.getLevel() + " " + SafeSimpleDateFormat.defaultDateFormat.format(logItem.getDate());

        if (!CollectionUtils.isEmpty(logItem.getMdcData())) {
            logStr += " " + logItem.getMdcData();
        }

        if (StringUtils.hasText(logItem.getClassName())) {
            String[] arr = $.string.split(logItem.getClassName(), '.');
            logStr += " " + arr[arr.length - 1];
        }

        if (StringUtils.hasText(logItem.getThreadName())) {
            logStr += " " + logItem.getThreadName();
        }

        if (logItem.getStackTraceElement() != null) {
            logStr += " " + logItem.getStackTraceElement();
        }

        logStr += " " + logItem.renderContentTemplate();
        return logStr;
    }
}
