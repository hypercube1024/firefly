package com.firefly.utils.log.file;

import com.firefly.utils.log.*;
import com.firefly.utils.time.SafeSimpleDateFormat;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class FileLog implements Log, Closeable {

    private LogLevel level;
    private String path;
    private String name;
    private boolean consoleOutput;
    private boolean fileOutput;
    private int maxFileSize;
    private int writedSize;
    private Charset charset = LogConfigParser.DEFAULT_CHARSET;
    private BufferedOutputStream bufferedOutputStream;
    private String currentDate = LogFactory.DAY_DATE_FORMAT.format(new Date());
    private static final int bufferSize = 4 * 1024;
    private static final boolean stackTrace = Boolean.getBoolean("debugMode");

    void write(LogItem logItem) {
        if (consoleOutput) {
            logItem.setDate(SafeSimpleDateFormat.defaultDateFormat.format(new Date()));
            System.out.println(logItem.toString());
        }

        if (fileOutput) {
            byte[] text = (logItem.toString() + CL).getBytes(charset);
            Date d = new Date();
            boolean success = initializeBufferedWriter(LogFactory.DAY_DATE_FORMAT.format(d), writedSize + text.length);
            if (success) {
                logItem.setDate(SafeSimpleDateFormat.defaultDateFormat.format(d));
                try {
                    bufferedOutputStream.write(text);
                    writedSize += text.length;
                } catch (IOException e) {
                    System.err.println("writer log exception, " + e.getMessage());
                }
            } else {
                System.err.println("The log " + toString() + " can not get buffered writer!");
            }
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

    @Override
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
            writedSize = (int) file.length();
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

    private boolean createNewLogFile(String date, int currentTextSize) {
        try {
            if (_createNewLogFile(date, currentTextSize)) {
                Files.createFile(Paths.get(path, getLogFileName(date)));
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return true;
        }
    }

    private boolean _createNewLogFile(String date, int currentTextSize) throws IOException {
        Path logPath = Paths.get(path, getLogFileName(date));
        if (!Files.exists(logPath)) {
            return true;
        } else {
            if (maxFileSize > 0) {
                if (!currentDate.equals(date)) {
                    return true;
                } else {
                    if (currentTextSize < 0 || currentTextSize > maxFileSize) {
                        // create log file backup
                        int index = 0;
                        while (Files.exists(Paths.get(path, getBackupLogFileName(date, index)))) {
                            index++;
                        }
                        Files.move(logPath, Paths.get(path, getBackupLogFileName(date, index)));
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                if (!currentDate.equals(date)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    private boolean initializeBufferedWriter(String newDate, int currentTextSize) {
        if (createNewLogFile(newDate, currentTextSize)) {
            close();
            return createNewBufferedOutputStream(newDate);
        } else {
            if (bufferedOutputStream == null) {
                return createNewBufferedOutputStream(newDate);
            } else {
                return true;
            }
        }
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

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(int maxFileSize) {
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
            add(str, LogLevel.TRACE.getName(), null, new Object[0]);
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
            add(str, LogLevel.DEBUG.getName(), null, new Object[0]);
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
            add(str, LogLevel.INFO.getName(), null, new Object[0]);
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
            add(str, LogLevel.WARN.getName(), null, new Object[0]);
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
            add(str, LogLevel.ERROR.getName(), null, new Object[0]);
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
        return "[level=" + level.getName() + ", name=" + name + ", path=" + path + ", consoleOutput=" + consoleOutput
                + ", fileOutput=" + fileOutput + ", maxFileSize=" + maxFileSize + "]";
    }

    public static StackTraceElement getStackTraceElement() {
        return Thread.currentThread().getStackTrace()[4];
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (consoleOutput ? 1231 : 1237);
        result = prime * result + (fileOutput ? 1231 : 1237);
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + maxFileSize;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FileLog other = (FileLog) obj;
        if (consoleOutput != other.consoleOutput)
            return false;
        if (fileOutput != other.fileOutput)
            return false;
        if (level != other.level)
            return false;
        if (maxFileSize != other.maxFileSize)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }

}
