package com.fireflysource.log;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public interface LogConfigParser {

    String DEFAULT_XML_CONFIG_FILE_NAME = "firefly-log.xml";
    String DEFAULT_LOG_NAME = "firefly-system";
    String DEFAULT_LOG_LEVEL = "INFO";
    long DEFAULT_MAX_FILE_SIZE = 209715200;
    File DEFAULT_LOG_DIRECTORY = new File(System.getProperty("user.dir"), "logs");
    boolean DEFAULT_CONSOLE_ENABLED = false;
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    MaxSplitTimeEnum DEFAULT_MAX_SPLIT_TIME = MaxSplitTimeEnum.DAY;
    LogFormatter DEFAULT_LOG_FORMATTER = new DefaultLogFormatter();
    LogNameFormatter DEFAULT_LOG_NAME_FORMATTER = new DefaultLogNameFormatter();
    LogFilter DEFAULT_LOG_FILTER = new DefaultLogFilter();

    boolean parse(Consumer<FileLog> consumer);

    FileLog createDefaultLog();

}
