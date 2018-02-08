package com.firefly.utils.log;

import com.firefly.utils.time.TimeUtils;

import java.time.LocalDate;

/**
 * @author Pengtao Qiu
 */
public class DefaultLogNameFormatter implements LogNameFormatter {

    @Override
    public String format(String name, LocalDate localDate) {
        return name + ".txt";
    }

    @Override
    public String formatBak(String name, LocalDate localDate, int index) {
        return name + "." + localDate.format(TimeUtils.DEFAULT_LOCAL_DATE) + "." + index + ".bak.txt";
    }
}
