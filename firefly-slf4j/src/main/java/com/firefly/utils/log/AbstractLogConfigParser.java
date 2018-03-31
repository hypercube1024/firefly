package com.firefly.utils.log;

import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.file.FileLog;

import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDate;

public abstract class AbstractLogConfigParser implements LogConfigParser {

    protected FileLog createLog(Configuration c) {
        FileLog fileLog = new FileLog();
        fileLog.setMaxLogFlushInterval(Math.max(c.getMaxLogFlushInterval(), 500L));
        fileLog.setName(c.getName());
        fileLog.setLevel(LogLevel.fromName(c.getLevel()));
        fileLog.setMaxFileSize(c.getMaxFileSize());
        fileLog.setCharset(Charset.forName(c.getCharset()));

        boolean success;
        if (VerifyUtils.isNotEmpty(c.getPath())) {
            File file = new File(c.getPath());
            success = createLogDirectory(file);
            if (success) {
                fileLog.setPath(c.getPath());
                fileLog.setFileOutput(true);
            } else {
                success = createLogDirectory(DEFAULT_LOG_DIRECTORY);
                if (success) {
                    fileLog.setPath(DEFAULT_LOG_DIRECTORY.getAbsolutePath());
                    fileLog.setFileOutput(true);
                } else {
                    fileLog.setFileOutput(false);
                }
            }
        } else {
            success = createLogDirectory(DEFAULT_LOG_DIRECTORY);
            if (success) {
                fileLog.setPath(DEFAULT_LOG_DIRECTORY.getAbsolutePath());
                fileLog.setFileOutput(true);
            } else {
                fileLog.setFileOutput(false);
            }
        }

        if (success) {
            fileLog.setConsoleOutput(c.isConsole());
        } else {
            fileLog.setConsoleOutput(true);
            System.err.println("create log directory is failure");
        }

        if (StringUtils.hasText(c.getFormatter())) {
            fileLog.setLogFormatter(new LogFormatter() {

                private LogFormatter formatter;

                @Override
                public String format(LogItem logItem) {
                    init();
                    return formatter.format(logItem);
                }

                private void init() {
                    try {
                        Class<?> clazz = AbstractLogConfigParser.class.getClassLoader().loadClass(c.getFormatter());
                        formatter = (LogFormatter) clazz.newInstance();
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                        e.printStackTrace();
                        formatter = new DefaultLogFormatter();
                    }
                }
            });
        }

        if (StringUtils.hasText(c.getLogNameFormatter())) {
            fileLog.setLogNameFormatter(new LogNameFormatter() {

                private LogNameFormatter formatter;

                @Override
                public String format(String name, LocalDate localDate) {
                    init();
                    return formatter.format(name, localDate);
                }

                @Override
                public String formatBak(String name, LocalDate localDate, int index) {
                    init();
                    return formatter.formatBak(name, localDate, index);
                }

                private void init() {
                    try {
                        Class<?> clazz = AbstractLogConfigParser.class.getClassLoader().loadClass(c.getLogNameFormatter());
                        formatter = (LogNameFormatter) clazz.newInstance();
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                        e.printStackTrace();
                        formatter = new DefaultLogNameFormatter();
                    }
                }

            });
        }

        if (StringUtils.hasText(c.getLogFilter())) {
            fileLog.setLogFilter(new LogFilter() {

                private LogFilter filter;

                @Override
                public void filter(LogItem logItem) {
                    init();
                    filter.filter(logItem);
                }

                private void init() {
                    if (filter == null) {
                        try {
                            Class<?> clazz = AbstractLogConfigParser.class.getClassLoader().loadClass(c.getLogFilter());
                            filter = (LogFilter) clazz.newInstance();
                        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                            e.printStackTrace();
                            filter = new DefaultLogFilter();
                        }
                    }
                }
            });
        }

        System.out.println("initialize log " + fileLog.toString());
        return fileLog;
    }

    private boolean createLogDirectory(File file) {
        return file.exists() && file.isDirectory() || file.mkdirs();
    }

    @Override
    public FileLog createDefaultLog() {
        Configuration c = new Configuration();
        c.setName(DEFAULT_LOG_NAME);
        c.setLevel(DEFAULT_LOG_LEVEL);
        c.setPath(DEFAULT_LOG_DIRECTORY.getAbsolutePath());
        c.setConsole(false);
        c.setMaxFileSize(DEFAULT_MAX_FILE_SIZE);
        c.setCharset(DEFAULT_CHARSET.name());
        c.setFormatter(DEFAULT_LOG_FORMATTER);
        return createLog(c);
    }

}
