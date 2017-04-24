package com.firefly.utils.log.file;

import com.firefly.utils.log.*;
import com.firefly.utils.time.SafeSimpleDateFormat;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;

public class FileLog implements Log, Closeable {

    private static final boolean stackTrace = Boolean.getBoolean("debugMode");

    private LogLevel level;
    private String path;
    private String name;
    private boolean consoleOutput;
    private boolean fileOutput;
    private long maxFileSize;
    private Charset charset = LogConfigParser.DEFAULT_CHARSET;

    private LogOutputStream output = new LogOutputStream();

    void write(LogItem logItem) {
        Date date = new Date();
        logItem.setDate(SafeSimpleDateFormat.defaultDateFormat.format(date));
        if (consoleOutput) {
            System.out.println(logItem.toString());
        }

        if (fileOutput) {
            logItem.setDate(SafeSimpleDateFormat.defaultDateFormat.format(date));
            output.write(logItem.toString(), date);
        }
    }

    private class LogOutputStream {

        private static final int bufferSize = 4 * 1024;
        private BufferedOutputStream bufferedOutputStream;

        private String currentDate = LogFactory.DAY_DATE_FORMAT.format(new Date());
        private long writeSize;
        private int currentBakIndex;

        public void write(String str, Date date) {
            byte[] text = (str + CL).getBytes(charset);
            boolean success = initializeBufferedWriter(LogFactory.DAY_DATE_FORMAT.format(date), writeSize + text.length);
            if (success) {
                try {
                    bufferedOutputStream.write(text);
                    writeSize += text.length;
                } catch (IOException e) {
                    System.err.println("writer log exception, " + e.getMessage());
                }
            } else {
                System.err.println("The log " + toString() + " can not get buffered writer!");
            }
        }

        public void flush() {
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.flush();
                } catch (IOException e) {
                    System.err.println("flush log buffer exception, " + e.getMessage());
                }
            }
        }

        public void close() {
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    System.err.println("close log writer exception, " + e.getMessage());
                }
            }
        }

        private boolean createNewBufferedOutputStream(String newDate) {
            try {
                File file = new File(path, getLogFileName(newDate));
                bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file, true), bufferSize);
                currentDate = newDate;
                writeSize = file.length();
                System.out.println("get new log buffer, the file path is " + file.getAbsolutePath() + " and the size is " + file.length());
                return true;
            } catch (IOException e) {
                System.err.println("create log writer exception, " + e.getMessage());
                return false;
            }
        }

        private String getLogFileName(String date) {
            return name + "." + date + ".txt";
        }

        private String getBackupLogFileName(String date, int index) {
            return getLogFileName(date) + "." + index + ".bak";
        }

        private boolean createNewLogFile(String date, long currentWriteSize) {
            boolean ret;
            try {
                Path logPath = Paths.get(path, getLogFileName(date));
                if (!Files.exists(logPath)) {
                    ret = true;
                } else {
                    if (maxFileSize > 0) {
                        if (!currentDate.equals(date)) {
                            ret = true;
                        } else {
                            if (currentWriteSize < 0 || currentWriteSize > maxFileSize) {
                                // create log file backup
                                while (Files.exists(Paths.get(path, getBackupLogFileName(date, currentBakIndex)))) {
                                    currentBakIndex++;
                                }
                                Files.move(logPath, Paths.get(path, getBackupLogFileName(date, currentBakIndex)));
                                ret = true;
                            } else {
                                ret = false;
                            }
                        }
                    } else {
                        ret = !currentDate.equals(date);
                    }
                }
                if (ret) {
                    Files.createFile(logPath);
                }
            } catch (IOException e) {
                System.err.println("create new log file exception, " + e.getMessage());
                ret = false;
            }
            return ret;
        }

        private boolean initializeBufferedWriter(String newDate, long currentWriteSize) {
            if (createNewLogFile(newDate, currentWriteSize)) {
                close();
                return createNewBufferedOutputStream(newDate);
            } else {
                return bufferedOutputStream != null || createNewBufferedOutputStream(newDate);
            }
        }
    }

    public void flush() {
        output.flush();
    }

    @Override
    public void close() {
        output.close();
    }

    public void setConsoleOutput(boolean consoleOutput) {
        this.consoleOutput = consoleOutput;
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

    private void add(String str, String level, Throwable throwable, Object... objs) {
        LogItem item = new LogItem();
        item.setLevel(level);
        item.setName(name);
        item.setContent(str);
        item.setObjs(objs);
        item.setThrowable(throwable);
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
                '}';
    }

    public static StackTraceElement getStackTraceElement() {
        StackTraceElement[] arr = Thread.currentThread().getStackTrace();
        StackTraceElement s = arr[4];
        if (s != null) {
            if (s.getClassName().equals("org.slf4j.impl.LoggerImpl")) {
                return arr[5];
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
