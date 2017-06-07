package com.firefly.utils.log;

import com.firefly.utils.StringUtils;
import com.firefly.utils.time.SafeSimpleDateFormat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;

public class LogItem {

    private String name, content, level;
    private Object[] objs;
    private Throwable throwable;
    private StackTraceElement stackTraceElement;
    private String logStr;
    private Map<String, String> mdcData;
    private Date date;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Object[] getObjs() {
        return objs;
    }

    public void setObjs(Object[] objs) {
        this.objs = objs;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public StackTraceElement getStackTraceElement() {
        return stackTraceElement;
    }

    public void setStackTraceElement(StackTraceElement stackTraceElement) {
        this.stackTraceElement = stackTraceElement;
    }

    public Map<String, String> getMdcData() {
        return mdcData;
    }

    public void setMdcData(Map<String, String> mdcData) {
        this.mdcData = mdcData;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        if (logStr == null) {
            content = StringUtils.replace(content, objs);
            if (throwable != null) {
                StringWriter str = new StringWriter();
                try (PrintWriter out = new PrintWriter(str)) {
                    out.println();
                    out.println("$err_start");
                    throwable.printStackTrace(out);
                    out.println("$err_end");
                }
                content += str.toString();
            }

            logStr = level + ", " + SafeSimpleDateFormat.defaultDateFormat.format(date);

            if (mdcData != null && !mdcData.isEmpty()) {
                logStr += ", " + mdcData;
            }

            if (stackTraceElement != null) {
                logStr += ", " + stackTraceElement;
            }

            logStr += ",\t" + content;
        }
        return logStr;
    }

}
