package com.fireflysource.net.http.model;

import com.fireflysource.common.string.StringUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * ThreadLocal Date formatters for HTTP style dates.
 */
public class DateGenerator {
    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    static {
        GMT_TIME_ZONE.setID("GMT");
    }

    static final String[] DAYS = {"Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
            "Jan"};

    private static final ThreadLocal<DateGenerator> DATE_GENERATOR = ThreadLocal.withInitial(DateGenerator::new);

    public final static String JAN_01_1970 = DateGenerator.formatDate(0);

    /**
     * Format HTTP date "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
     *
     * @param date the date in milliseconds
     * @return the formatted date
     */
    public static String formatDate(long date) {
        return DATE_GENERATOR.get().doFormatDate(date);
    }

    /**
     * Format "EEE, dd-MMM-yyyy HH:mm:ss 'GMT'" for cookies
     *
     * @param buf  the buffer to put the formatted date into
     * @param date the date in milliseconds
     */
    public static void formatCookieDate(StringBuilder buf, long date) {
        DATE_GENERATOR.get().doFormatCookieDate(buf, date);
    }

    /**
     * Format "EEE, dd-MMM-yyyy HH:mm:ss 'GMT'" for cookies
     *
     * @param date the date in milliseconds
     * @return the formatted date
     */
    public static String formatCookieDate(long date) {
        StringBuilder buf = new StringBuilder(28);
        formatCookieDate(buf, date);
        return buf.toString();
    }

    private final StringBuilder buf = new StringBuilder(32);
    private final GregorianCalendar gc = new GregorianCalendar(GMT_TIME_ZONE);

    /**
     * Format HTTP date "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
     *
     * @param date the date in milliseconds
     * @return the formatted date
     */
    public String doFormatDate(long date) {
        buf.setLength(0);
        gc.setTimeInMillis(date);

        int day_of_week = gc.get(Calendar.DAY_OF_WEEK);
        int day_of_month = gc.get(Calendar.DAY_OF_MONTH);
        int month = gc.get(Calendar.MONTH);
        int year = gc.get(Calendar.YEAR);
        int century = year / 100;
        year = year % 100;

        int hours = gc.get(Calendar.HOUR_OF_DAY);
        int minutes = gc.get(Calendar.MINUTE);
        int seconds = gc.get(Calendar.SECOND);

        buf.append(DAYS[day_of_week]);
        buf.append(',');
        buf.append(' ');
        StringUtils.append2digits(buf, day_of_month);

        buf.append(' ');
        buf.append(MONTHS[month]);
        buf.append(' ');
        StringUtils.append2digits(buf, century);
        StringUtils.append2digits(buf, year);

        buf.append(' ');
        StringUtils.append2digits(buf, hours);
        buf.append(':');
        StringUtils.append2digits(buf, minutes);
        buf.append(':');
        StringUtils.append2digits(buf, seconds);
        buf.append(" GMT");
        return buf.toString();
    }

    /**
     * Format "EEE, dd-MMM-yy HH:mm:ss 'GMT'" for cookies
     *
     * @param buf  the buffer to format the date into
     * @param date the date in milliseconds
     */
    public void doFormatCookieDate(StringBuilder buf, long date) {
        gc.setTimeInMillis(date);

        int day_of_week = gc.get(Calendar.DAY_OF_WEEK);
        int day_of_month = gc.get(Calendar.DAY_OF_MONTH);
        int month = gc.get(Calendar.MONTH);
        int year = gc.get(Calendar.YEAR);
        year = year % 10000;

        int epoch = (int) ((date / 1000) % (60 * 60 * 24));
        int seconds = epoch % 60;
        epoch = epoch / 60;
        int minutes = epoch % 60;
        int hours = epoch / 60;

        buf.append(DAYS[day_of_week]);
        buf.append(',');
        buf.append(' ');
        StringUtils.append2digits(buf, day_of_month);

        buf.append('-');
        buf.append(MONTHS[month]);
        buf.append('-');
        StringUtils.append2digits(buf, year / 100);
        StringUtils.append2digits(buf, year % 100);

        buf.append(' ');
        StringUtils.append2digits(buf, hours);
        buf.append(':');
        StringUtils.append2digits(buf, minutes);
        buf.append(':');
        StringUtils.append2digits(buf, seconds);
        buf.append(" GMT");
    }
}
