package com.fireflysource.net.http.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * ThreadLocal data parsers for HTTP style dates
 */
public class DateParser {
    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    static {
        GMT_TIME_ZONE.setID("GMT");
    }

    final static String[] DATE_RECEIVE_FMT = {
            "EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, dd-MMM-yy HH:mm:ss",
            "EEE MMM dd HH:mm:ss yyyy",

            "EEE, dd MMM yyyy HH:mm:ss", "EEE dd MMM yyyy HH:mm:ss zzz", "EEE dd MMM yyyy HH:mm:ss",
            "EEE MMM dd yyyy HH:mm:ss zzz", "EEE MMM dd yyyy HH:mm:ss", "EEE MMM-dd-yyyy HH:mm:ss zzz",
            "EEE MMM-dd-yyyy HH:mm:ss", "dd MMM yyyy HH:mm:ss zzz", "dd MMM yyyy HH:mm:ss", "dd-MMM-yy HH:mm:ss zzz",
            "dd-MMM-yy HH:mm:ss", "MMM dd HH:mm:ss yyyy zzz", "MMM dd HH:mm:ss yyyy", "EEE MMM dd HH:mm:ss yyyy zzz",
            "EEE, MMM dd HH:mm:ss yyyy zzz", "EEE, MMM dd HH:mm:ss yyyy", "EEE, dd-MMM-yy HH:mm:ss zzz",
            "EEE dd-MMM-yy HH:mm:ss zzz", "EEE dd-MMM-yy HH:mm:ss"};

    public static long parseDate(String date) {
        return DATE_PARSER.get().parse(date);
    }

    private static final ThreadLocal<DateParser> DATE_PARSER = ThreadLocal.withInitial(DateParser::new);

    final SimpleDateFormat[] DATE_RECEIVE = new SimpleDateFormat[DATE_RECEIVE_FMT.length];

    private long parse(final String dateVal) {
        for (int i = 0; i < DATE_RECEIVE.length; i++) {
            if (DATE_RECEIVE[i] == null) {
                DATE_RECEIVE[i] = new SimpleDateFormat(DATE_RECEIVE_FMT[i], Locale.US);
                DATE_RECEIVE[i].setTimeZone(GMT_TIME_ZONE);
            }

            try {
                Date date = (Date) DATE_RECEIVE[i].parseObject(dateVal);
                return date.getTime();
            } catch (Exception ignored) {
            }
        }

        if (dateVal.endsWith(" GMT")) {
            final String val = dateVal.substring(0, dateVal.length() - 4);

            for (SimpleDateFormat element : DATE_RECEIVE) {
                try {
                    Date date = (Date) element.parseObject(val);
                    return date.getTime();
                } catch (Exception ignored) {
                }
            }
        }
        return -1;
    }
}
