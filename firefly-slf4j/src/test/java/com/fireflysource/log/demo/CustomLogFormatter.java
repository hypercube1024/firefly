package com.fireflysource.log.demo;


import com.fireflysource.log.LogFormatter;
import com.fireflysource.log.LogItem;
import com.fireflysource.log.internal.utils.StringUtils;
import com.fireflysource.log.internal.utils.TimeUtils;

import static com.fireflysource.log.internal.utils.TimeUtils.DEFAULT_LOCAL_DATE_TIME;

/**
 * @author Pengtao Qiu
 */
public class CustomLogFormatter implements LogFormatter {

    @Override
    public String format(LogItem logItem) {
        String logStr = logItem.getLevel() + " " + TimeUtils.format(logItem.getDate(), DEFAULT_LOCAL_DATE_TIME);

        if (logItem.getMdcData() != null && !logItem.getMdcData().isEmpty()) {
            logStr += " " + logItem.getMdcData();
        }

        if (StringUtils.hasText(logItem.getClassName())) {
            logStr += " " + logItem.getClassName();
        }

        if (StringUtils.hasText(logItem.getThreadName())) {
            logStr += " " + logItem.getThreadName();
        }

        logStr += " --> " + logItem.renderContentTemplate();
        return logStr;
    }

}
