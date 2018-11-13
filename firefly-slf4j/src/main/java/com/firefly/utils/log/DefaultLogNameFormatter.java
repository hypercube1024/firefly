package com.firefly.utils.log;

import com.firefly.utils.time.TimeUtils;

import java.time.LocalDateTime;

/**
 * @author Pengtao Qiu
 */
public class DefaultLogNameFormatter implements LogNameFormatter {

    @Override
    public String format(String name, LocalDateTime localDateTime) {
        return name + ".txt";
    }

    @Override
    public String formatBak(String name, LocalDateTime localDateTime, int index) {
        return name + "." + localDateTime.format(TimeUtils.FILE_NAME_LOCAL_DATE_TIME) + "." + index + ".bak.txt";
    }
}
