package com.fireflysource.log;

import com.fireflysource.log.internal.utils.StringUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDateTime;

public abstract class AbstractLogConfigParser implements LogConfigParser {

    protected FileLog createLog(Configuration c) {
        FileLog fileLog = new FileLog();
        fileLog.setLogName(c.getName());
        fileLog.setLevel(LogLevel.fromName(c.getLevel()));
        fileLog.setMaxFileSize(c.getMaxFileSize());
        fileLog.setCharset(Charset.forName(c.getCharset()));

        boolean success;
        if (StringUtils.hasText(c.getPath())) {
            File file = new File(c.getPath());
            success = createLogDirectory(file);
            if (success) {
                fileLog.setPath(c.getPath());
                fileLog.setFileOutput(true);
            } else {
                success = createDefaultLogDir(fileLog);
            }
        } else {
            success = createDefaultLogDir(fileLog);
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
                    if (formatter == null) {
                        try {
                            Class<?> clazz = AbstractLogConfigParser.class.getClassLoader().loadClass(c.getFormatter());
                            formatter = (LogFormatter) clazz.newInstance();
                        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                            e.printStackTrace();
                            formatter = DEFAULT_LOG_FORMATTER;
                        }
                    }
                }
            });
        }

        if (StringUtils.hasText(c.getLogNameFormatter())) {
            fileLog.setLogNameFormatter(new LogNameFormatter() {

                private LogNameFormatter formatter;

                @Override
                public String format(String name, LocalDateTime localDateTime) {
                    init();
                    return formatter.format(name, localDateTime);
                }

                @Override
                public String formatBak(String name, LocalDateTime localDateTime, int index) {
                    init();
                    return formatter.formatBak(name, localDateTime, index);
                }

                private void init() {
                    if (formatter == null) {
                        try {
                            Class<?> clazz = AbstractLogConfigParser.class.getClassLoader().loadClass(c.getLogNameFormatter());
                            formatter = (LogNameFormatter) clazz.newInstance();
                        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                            e.printStackTrace();
                            formatter = DEFAULT_LOG_NAME_FORMATTER;
                        }
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
                            filter = DEFAULT_LOG_FILTER;
                        }
                    }
                }
            });
        }

        if (StringUtils.hasText(c.getMaxSplitTime())) {
            fileLog.setMaxSplitTime(MaxSplitTimeEnum.from(c.getMaxSplitTime()).orElse(DEFAULT_MAX_SPLIT_TIME));
        } else {
            fileLog.setMaxSplitTime(DEFAULT_MAX_SPLIT_TIME);
        }

        System.out.println("initialize log " + fileLog.toString());
        return fileLog;
    }

    private boolean createLogDirectory(File file) {
        return file.exists() && file.isDirectory() || file.mkdirs();
    }

    private boolean createDefaultLogDir(FileLog fileLog) {
        boolean success = createLogDirectory(DEFAULT_LOG_DIRECTORY);
        if (success) {
            fileLog.setPath(DEFAULT_LOG_DIRECTORY.getAbsolutePath());
            fileLog.setFileOutput(true);
        } else {
            fileLog.setFileOutput(false);
        }
        return success;
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
        c.setMaxSplitTime(DEFAULT_MAX_SPLIT_TIME.getValue());
        c.setFormatter(DEFAULT_LOG_FORMATTER.getClass().getName());
        c.setLogNameFormatter(DEFAULT_LOG_NAME_FORMATTER.getClass().getName());
        c.setLogFilter(DEFAULT_LOG_FILTER.getClass().getName());
        return createLog(c);
    }

}
