package com.firefly.utils.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.*;

/**
 * @author Pengtao Qiu
 */
public class DefaultLogNameFormatter implements LogNameFormatter {

    public static final DateTimeFormatter DEFAULT_LOG_NAME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral('_')
            .append(new DateTimeFormatterBuilder()
                    .appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral('-')
                    .appendValue(MINUTE_OF_HOUR, 2)
                    .optionalStart()
                    .appendLiteral('-')
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .toFormatter())
            .toFormatter();

    @Override
    public String format(String name, LocalDateTime localDateTime) {
        return name + ".txt";
    }

    @Override
    public String formatBak(String name, LocalDateTime localDateTime, int index) {
        return name + "." + localDateTime.format(DEFAULT_LOG_NAME_FORMATTER) + "." + index + ".bak.txt";
    }
}
