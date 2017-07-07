package com.firefly.kotlin.ext

import com.firefly.utils.StringUtils
import com.firefly.utils.log.LogFormatter
import com.firefly.utils.log.LogItem
import com.firefly.utils.time.SafeSimpleDateFormat

/**
 * @author Pengtao Qiu
 */
class KotlinTestLogFormatter : LogFormatter {
    override fun format(logItem: LogItem): String? {
        var logStr = logItem.level + " " + SafeSimpleDateFormat.defaultDateFormat.format(logItem.date)

        if (logItem.mdcData != null && !logItem.mdcData.isEmpty()) {
            logStr += " " + logItem.mdcData
        }

        if (StringUtils.hasText(logItem.className)) {
            logStr += " " + logItem.className
        }

        if (StringUtils.hasText(logItem.threadName)) {
            logStr += " " + logItem.threadName
        }

        if (logItem.stackTraceElement != null) {
            logStr += " " + logItem.stackTraceElement
        }

        logStr += " --> " + logItem.renderContentTemplate()
        return logStr
    }
}