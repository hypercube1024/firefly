package org.firefly.slf4j;

import com.firefly.utils.StringUtils;
import com.firefly.utils.log.LogFormatter;
import com.firefly.utils.log.LogItem;
import com.firefly.utils.time.SafeSimpleDateFormat;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Pengtao Qiu
 */
public class CustomLogFormatter implements LogFormatter {

    @Override
    public String format(LogItem logItem) {
        String content = StringUtils.replace(logItem.getContent(), logItem.getObjs());
        if (logItem.getThrowable() != null) {
            StringWriter str = new StringWriter();
            try (PrintWriter out = new PrintWriter(str)) {
                out.println();
                out.println("$err_start");
                logItem.getThrowable().printStackTrace(out);
                out.println("$err_end");
            }
            content += str.toString();
        }

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

        logStr += " --->>> " + content;
        return logStr;
    }

}
