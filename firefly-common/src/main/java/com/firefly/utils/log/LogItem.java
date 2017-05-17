package com.firefly.utils.log;

import com.firefly.utils.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogItem {

    private String name, content, date, level, requestId;
    private Object[] objs;
    private Throwable throwable;
    private StackTraceElement stackTraceElement;
    private String logStr;

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public void setObjs(Object[] objs) {
        this.objs = objs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setDate(String date) {
        this.date = date;
    }

    public StackTraceElement getStackTraceElement() {
        return stackTraceElement;
    }

    public void setStackTraceElement(StackTraceElement stackTraceElement) {
        this.stackTraceElement = stackTraceElement;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

            logStr = level + ", " + date;

            if (requestId != null) {
                logStr += ", " + requestId;
            }

            if (stackTraceElement != null) {
                logStr += ", " + stackTraceElement;
            }

            logStr += "\t" + content;
        }
        return logStr;
    }

}
