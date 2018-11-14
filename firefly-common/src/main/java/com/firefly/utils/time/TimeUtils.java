package com.firefly.utils.time;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Date;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.*;

/**
 * @author Pengtao Qiu
 */
abstract public class TimeUtils {

    public static final DateTimeFormatter LOCAL_DATE_SLASH_SEPARATOR = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('/')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('/')
            .appendValue(DAY_OF_MONTH, 2)
            .toFormatter();

    public static final DateTimeFormatter DEFAULT_LOCAL_MONTH = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .toFormatter();

    public static final DateTimeFormatter DEFAULT_LOCAL_DATE = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(DAY_OF_MONTH, 2)
            .toFormatter();

    public static final DateTimeFormatter DEFAULT_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(new DateTimeFormatterBuilder()
                    .appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral(':')
                    .appendValue(MINUTE_OF_HOUR, 2)
                    .optionalStart()
                    .appendLiteral(':')
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .toFormatter())
            .toFormatter();

    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(new DateTimeFormatterBuilder()
                    .appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral(':')
                    .appendValue(MINUTE_OF_HOUR, 2)
                    .optionalStart()
                    .appendLiteral(':')
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .toFormatter())
            .appendLiteral('Z')
            .toFormatter();

    public static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate toLocalDate(Date date) {
        return LocalDate.from(date.toInstant().atZone(ZoneId.systemDefault()));
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.from(date.toInstant().atZone(ZoneId.systemDefault()));
    }

    public static String format(Date date, DateTimeFormatter formatter) {
        return date.toInstant().atZone(ZoneId.systemDefault()).format(formatter);
    }

    public static String format(Date date, ZoneOffset offset, DateTimeFormatter formatter) {
        return date.toInstant().atOffset(offset).format(formatter);
    }

    public static Date parseLocalDate(String text, DateTimeFormatter formatter) {
        return toDate(LocalDate.parse(text, formatter));
    }

    public static Date parseLocalDateTime(String text, DateTimeFormatter formatter) {
        return toDate(LocalDateTime.parse(text, formatter));
    }

    public static LocalDate parseYearMonth(String text, DateTimeFormatter formatter) {
        return YearMonth.parse(text, formatter).atDay(1);
    }

    public static long between(ChronoUnit chronoUnit, Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
        return chronoUnit.between(temporal1Inclusive, temporal2Exclusive);
    }
}
