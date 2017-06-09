package org.firefly.slf4j;

import com.firefly.utils.StringUtils;
import com.firefly.utils.log.LogFormatter;
import com.firefly.utils.log.LogItem;
import com.firefly.utils.time.SafeSimpleDateFormat;

/**
 * @author Pengtao Qiu
 */
public class CustomLogFormatter implements LogFormatter {

    @Override
    public String format(LogItem logItem) {
        String logStr = logItem.getLevel() + " >>> " + SafeSimpleDateFormat.defaultDateFormat.format(logItem.getDate());

        if (logItem.getMdcData() != null && !logItem.getMdcData().isEmpty()) {
            logStr += " >>> " + logItem.getMdcData();
        }

        if (StringUtils.hasText(logItem.getClassName())) {
            logStr += " >>> " + logItem.getClassName();
        }

        if (StringUtils.hasText(logItem.getThreadName())) {
            logStr += " >>> " + logItem.getThreadName();
        }

        logStr += " --->>> " + logItem.renderContentTemplate();
        return logStr;
    }

}
