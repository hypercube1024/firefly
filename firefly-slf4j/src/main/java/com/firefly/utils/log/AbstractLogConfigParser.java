package com.firefly.utils.log;

import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.file.FileLog;

import java.io.File;
import java.nio.charset.Charset;

public abstract class AbstractLogConfigParser implements LogConfigParser {

    protected FileLog createLog(Configuration c) {
        FileLog fileLog = new FileLog();
        fileLog.setMaxLogFlushInterval(Math.max(c.getMaxLogFlushInterval(), 500L));
        fileLog.setName(c.getName());
        fileLog.setLevel(LogLevel.fromName(c.getLevel()));
        fileLog.setMaxFileSize(c.getMaxFileSize());
        fileLog.setCharset(Charset.forName(c.getCharset()));

        boolean createLogDirectorySuccess;
        if (VerifyUtils.isNotEmpty(c.getPath())) {
            File file = new File(c.getPath());
            createLogDirectorySuccess = createLogDirectory(file);
            if (createLogDirectorySuccess) {
                fileLog.setPath(c.getPath());
                fileLog.setFileOutput(true);
            } else {
                createLogDirectorySuccess = createLogDirectory(DEFAULT_LOG_DIRECTORY);
                if (createLogDirectorySuccess) {
                    fileLog.setPath(DEFAULT_LOG_DIRECTORY.getAbsolutePath());
                    fileLog.setFileOutput(true);
                } else {
                    fileLog.setFileOutput(false);
                }
            }
        } else {
            createLogDirectorySuccess = createLogDirectory(DEFAULT_LOG_DIRECTORY);
            if (createLogDirectorySuccess) {
                fileLog.setPath(DEFAULT_LOG_DIRECTORY.getAbsolutePath());
                fileLog.setFileOutput(true);
            } else {
                fileLog.setFileOutput(false);
            }
        }

        if (createLogDirectorySuccess) {
            fileLog.setConsoleOutput(c.isConsole());
        } else {
            fileLog.setConsoleOutput(true);
            System.err.println("create log directory is failure");
        }

        if (StringUtils.hasText(c.getFormatter())) {
            try {
                Class<?> clazz = AbstractLogConfigParser.class.getClassLoader().loadClass(c.getFormatter());
                fileLog.setLogFormatter((LogFormatter) clazz.newInstance());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
                fileLog.setLogFormatter(new DefaultLogFormatter());
            }
        }

        if (StringUtils.hasText(c.getLogNameFormatter())) {
            try {
                Class<?> clazz = AbstractLogConfigParser.class.getClassLoader().loadClass(c.getLogNameFormatter());
                fileLog.setLogNameFormatter((LogNameFormatter) clazz.newInstance());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
                fileLog.setLogNameFormatter(new DefaultLogNameFormatter());
            }
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
