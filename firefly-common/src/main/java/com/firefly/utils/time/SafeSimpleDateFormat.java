package com.firefly.utils.time;

import com.firefly.utils.VerifyUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SafeSimpleDateFormat {

    public static final SafeSimpleDateFormat defaultDateFormat = new SafeSimpleDateFormat();
    private ThreadLocal<SimpleDateFormat> threadLocal;

    public SafeSimpleDateFormat() {
        this("");
    }

    public SafeSimpleDateFormat(final SimpleDateFormat sdf) {
        if (sdf == null)
            throw new IllegalArgumentException("SimpleDateFormat argument is null");
        this.threadLocal = ThreadLocal.withInitial(() -> (SimpleDateFormat) sdf.clone());
    }

    public SafeSimpleDateFormat(String datePattern) {
        final String p = VerifyUtils.isEmpty(datePattern) ? "yyyy-MM-dd HH:mm:ss" : datePattern;
        this.threadLocal = ThreadLocal.withInitial(() -> new SimpleDateFormat(p));
    }

    public Date parse(String dateStr) {
        try {
            return getFormat().parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    public String format(Date date) {
        return getFormat().format(date);
    }

    private DateFormat getFormat() {
        return threadLocal.get();
    }

}
