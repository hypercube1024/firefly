package com.firefly.utils.log;

/**
 * @author Pengtao Qiu
 */
public class Configuration {

    private String name;
    private String level;
    private String path;
    private boolean console;
    private long maxFileSize;
    private String charset;
    private String formatter;
    private long maxLogFlushInterval = Long.getLong("com.firefly.utils.log.file.maxLogFlushInterval", 1000L);
    private String logNameFormatter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isConsole() {
        return console;
    }

    public void setConsole(boolean console) {
        this.console = console;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getFormatter() {
        return formatter;
    }

    public void setFormatter(String formatter) {
        this.formatter = formatter;
    }

    public long getMaxLogFlushInterval() {
        return maxLogFlushInterval;
    }

    public void setMaxLogFlushInterval(long maxLogFlushInterval) {
        this.maxLogFlushInterval = maxLogFlushInterval;
    }

    public String getLogNameFormatter() {
        return logNameFormatter;
    }

    public void setLogNameFormatter(String logNameFormatter) {
        this.logNameFormatter = logNameFormatter;
    }
}
