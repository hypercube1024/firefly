package com.firefly.utils.log.file;

import com.firefly.utils.log.*;
import com.firefly.utils.time.Millisecond100Clock;
import com.firefly.utils.time.TimeUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public class FileLog implements Log, Closeable {

    private static final boolean stackTrace = Boolean.getBoolean("com.firefly.utils.log.file.debugMode");

    private LogLevel level;
    private String path;
    private String name;
    private boolean consoleOutput;
    private boolean fileOutput;
    private long maxFileSize;
    private Charset charset = LogConfigParser.DEFAULT_CHARSET;
    private LogFormatter logFormatter;
    private long lastFlushTime;
    private long maxLogFlushInterval;
    private LogNameFormatter logNameFormatter;
    private LogFilter logFilter;
    private MappedDiagnosticContext mdc;
    private LogOutputStream output = new LogOutputStream();
    private MaxSplitTimeEnum maxSplitTime;

    public FileLog() {
        mdc = MappedDiagnosticContextFactory.getInstance().getMappedDiagnosticContext();
    }

    void write(LogItem logItem) {
        Optional.ofNullable(logFilter).ifPresent(f -> f.filter(logItem));

        if (consoleOutput) {
            System.out.println(logFormatter.format(logItem));
        }

        if (fileOutput) {
            output.write(logFormatter.format(logItem), logItem.getDate());
            intervalFlush();
        }
    }

    private class LogOutputStream {

        private static final int bufferSize = 4 * 1024;
        private BufferedOutputStream bufferedOutputStream;
        private long writeSize;
        private LocalDateTime lastWriteTime;

        private String getLogName(LocalDateTime localDateTime) {
            return logNameFormatter.format(name, localDateTime);
        }

        private String getLogBakName(LocalDateTime localDateTime, int index) {
            return logNameFormatter.formatBak(name, localDateTime, index);
        }

        private String getLogBakName(LocalDateTime localDateTime) {
            int index = 0;
            String bakName = getLogBakName(localDateTime, index);
            while (Files.exists(Paths.get(path, bakName))) {
                index++;
                bakName = getLogBakName(localDateTime, index);
            }
            return bakName;
        }

        private boolean isNotOverTimeLimit(LocalDateTime newLocalDateTime) {
            switch (maxSplitTime) {
                case DAY: {
                    return lastWriteTime.getYear() == newLocalDateTime.getYear()
                            && lastWriteTime.getMonth() == newLocalDateTime.getMonth()
                            && lastWriteTime.getDayOfMonth() == newLocalDateTime.getDayOfMonth();
                }
                case HOUR: {
                    return lastWriteTime.getYear() == newLocalDateTime.getYear()
                            && lastWriteTime.getMonth() == newLocalDateTime.getMonth()
                            && lastWriteTime.getDayOfMonth() == newLocalDateTime.getDayOfMonth()
                            && lastWriteTime.getHour() == newLocalDateTime.getHour();
                }
                case MINUTE: {
                    return lastWriteTime.getYear() == newLocalDateTime.getYear()
                            && lastWriteTime.getMonth() == newLocalDateTime.getMonth()
                            && lastWriteTime.getDayOfMonth() == newLocalDateTime.getDayOfMonth()
                            && lastWriteTime.getHour() == newLocalDateTime.getHour()
                            && lastWriteTime.getMinute() == newLocalDateTime.getMinute();
                }
            }
            return true;
        }

        private void initializeBufferedWriter(Date newDate, long currentWriteSize) throws IOException {
            LocalDateTime newLocalDateTime = TimeUtils.toLocalDateTime(newDate);
            String logName = getLogName(newLocalDateTime);
            Path logPath = Paths.get(path, logName);

            if (Files.exists(logPath)) {
                if (lastWriteTime == null) {
                    FileTime fileTime = Files.getLastModifiedTime(logPath);
                    lastWriteTime = LocalDateTime.from(fileTime.toInstant().atZone(ZoneId.systemDefault()));
                }

                if (!isNotOverTimeLimit(newLocalDateTime)) {
                    initOutputStreamAndNewFile(logName, logPath, lastWriteTime);
                } else {
                    if (maxFileSize > 0) {
                        if (writeSize == 0) {
                            writeSize = Files.size(logPath);
                        }
                        if ((currentWriteSize + writeSize) > maxFileSize) {
                            initOutputStreamAndNewFile(logName, logPath, lastWriteTime);
                        } else {
                            initOutputStream(logName);
                        }
                    } else {
                        initOutputStream(logName);
                    }
                }
            } else {
                initOutputStream(logName);
            }
        }

        private void initOutputStreamAndNewFile(String logName, Path logPath, LocalDateTime fileLastModifiedDateTime) throws IOException {
            close();
            Files.move(logPath, Paths.get(path, getLogBakName(fileLastModifiedDateTime)));
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(path, logName), true), bufferSize);
        }

        private void initOutputStream(String logName) throws FileNotFoundException {
            if (bufferedOutputStream == null) {
                bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(path, logName), true), bufferSize);
            }
        }

        public void write(String str, Date date) {
            byte[] text = (str + CL).getBytes(charset);
            try {
                initializeBufferedWriter(date, text.length);
                bufferedOutputStream.write(text);
                writeSize += text.length;
                lastWriteTime = TimeUtils.toLocalDateTime(date);
            } catch (IOException e) {
                System.err.println("write log exception, " + e.getMessage());
            }
        }

        public void flush() {
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.flush();
                    lastFlushTime = Millisecond100Clock.currentTimeMillis();
                } catch (IOException e) {
                    System.err.println("flush log buffer exception, " + e.getMessage());
                }
            }
        }

        public void close() {
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                    writeSize = 0;
                } catch (IOException e) {
                    System.err.println("close log writer exception, " + e.getMessage());
                }
            }
        }

    }

    public void flush() {
        output.flush();
    }

    public void intervalFlush() {
        long interval = Millisecond100Clock.currentTimeMillis() - lastFlushTime;
        if (interval > maxLogFlushInterval) {
            flush();
        }
    }

    @Override
    public void close() {
        output.close();
    }

    public boolean isConsoleOutput() {
        return consoleOutput;
    }

    public void setConsoleOutput(boolean consoleOutput) {
        this.consoleOutput = consoleOutput;
    }

    public boolean isFileOutput() {
        return fileOutput;
    }

    public void setFileOutput(boolean fileOutput) {
        this.fileOutput = fileOutput;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public LogFormatter getLogFormatter() {
        return logFormatter;
    }

    public void setLogFormatter(LogFormatter logFormatter) {
        this.logFormatter = logFormatter;
    }

    public long getMaxLogFlushInterval() {
        return maxLogFlushInterval;
    }

    public void setMaxLogFlushInterval(long maxLogFlushInterval) {
        this.maxLogFlushInterval = maxLogFlushInterval;
    }

    public LogNameFormatter getLogNameFormatter() {
        return logNameFormatter;
    }

    public void setLogNameFormatter(LogNameFormatter logNameFormatter) {
        this.logNameFormatter = logNameFormatter;
    }

    public LogFilter getLogFilter() {
        return logFilter;
    }

    public void setLogFilter(LogFilter logFilter) {
        this.logFilter = logFilter;
    }

    public MaxSplitTimeEnum getMaxSplitTime() {
        return maxSplitTime;
    }

    public void setMaxSplitTime(MaxSplitTimeEnum maxSplitTime) {
        this.maxSplitTime = maxSplitTime;
    }

    private void add(String str, String level, Throwable throwable, Object... objs) {
        LogItem item = new LogItem();
        item.setLevel(level);
        item.setName(name);
        item.setContent(str);
        item.setObjs(objs);
        item.setThrowable(throwable);
        item.setDate(new Date());
        item.setMdcData(mdc.getCopyOfContextMap());
        item.setClassName(ClassNameLogWrap.name.get());
        item.setThreadName(Thread.currentThread().getName());
        if (stackTrace) {
            item.setStackTraceElement(getStackTraceElement());
        }
        LogFactory.getInstance().getLogTask().add(item);
    }

    @Override
    public void trace(String str) {
        if (isTraceEnabled()) {
            add(str, LogLevel.TRACE.getName(), null);
        }
    }

    @Override
    public void trace(String str, Object... objs) {
        if (isTraceEnabled()) {
            add(str, LogLevel.TRACE.getName(), null, objs);
        }
    }

    @Override
    public void trace(String str, Throwable throwable, Object... objs) {
        if (isTraceEnabled()) {
            add(str, LogLevel.TRACE.getName(), null, objs);
        }
    }

    @Override
    public void debug(String str) {
        if (isDebugEnabled()) {
            add(str, LogLevel.DEBUG.getName(), null);
        }
    }

    @Override
    public void debug(String str, Object... objs) {
        if (isDebugEnabled()) {
            add(str, LogLevel.DEBUG.getName(), null, objs);
        }
    }

    @Override
    public void debug(String str, Throwable throwable, Object... objs) {
        if (isDebugEnabled()) {
            add(str, LogLevel.DEBUG.getName(), throwable, objs);
        }
    }

    @Override
    public void info(String str) {
        if (isInfoEnabled()) {
            add(str, LogLevel.INFO.getName(), null);
        }
    }

    @Override
    public void info(String str, Object... objs) {
        if (isInfoEnabled()) {
            add(str, LogLevel.INFO.getName(), null, objs);
        }
    }

    @Override
    public void info(String str, Throwable throwable, Object... objs) {
        if (isInfoEnabled()) {
            add(str, LogLevel.INFO.getName(), throwable, objs);
        }
    }

    @Override
    public void warn(String str) {
        if (isWarnEnabled()) {
            add(str, LogLevel.WARN.getName(), null);
        }
    }

    @Override
    public void warn(String str, Object... objs) {
        if (isWarnEnabled()) {
            add(str, LogLevel.WARN.getName(), null, objs);
        }
    }

    @Override
    public void warn(String str, Throwable throwable, Object... objs) {
        if (isWarnEnabled()) {
            add(str, LogLevel.WARN.getName(), throwable, objs);
        }
    }

    @Override
    public void error(String str, Object... objs) {
        if (isErrorEnabled()) {
            add(str, LogLevel.ERROR.getName(), null, objs);
        }
    }

    @Override
    public void error(String str, Throwable throwable, Object... objs) {
        if (isErrorEnabled()) {
            add(str, LogLevel.ERROR.getName(), throwable, objs);
        }
    }

    @Override
    public void error(String str) {
        if (isErrorEnabled()) {
            add(str, LogLevel.ERROR.getName(), null);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return level.isEnabled(LogLevel.TRACE);
    }

    @Override
    public boolean isDebugEnabled() {
        return level.isEnabled(LogLevel.DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return level.isEnabled(LogLevel.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return level.isEnabled(LogLevel.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return level.isEnabled(LogLevel.ERROR);
    }

    @Override
    public String toString() {
        return "FileLog{" +
                "level=" + level +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", consoleOutput=" + consoleOutput +
                ", fileOutput=" + fileOutput +
                ", maxFileSize=" + maxFileSize +
                ", charset=" + charset +
                ", logFormatter=" + logFormatter.getClass().getName() +
                ", maxLogFlushInterval=" + maxLogFlushInterval +
                ", maxSplitTime=" + maxSplitTime.getValue() +
                '}';
    }

    public static StackTraceElement getStackTraceElement() {
        StackTraceElement[] arr = Thread.currentThread().getStackTrace();
        StackTraceElement s = arr[4];
        if (s != null) {
            if (s.getClassName().equals("com.firefly.utils.log.ClassNameLogWrap")) {
                return arr[6];
            } else {
                return s;
            }
        } else {
            return s;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileLog fileLog = (FileLog) o;
        return Objects.equals(name, fileLog.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
