package com.firefly.utils.log;

import com.firefly.utils.function.Action1;
import com.firefly.utils.log.file.FileLog;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface LogConfigParser {

    String DEFAULT_XML_CONFIG_FILE_NAME = "firefly-log.xml";
    String DEFAULT_LOG_NAME = "firefly-system";
    String DEFAULT_LOG_LEVEL = "INFO";
    long DEFAULT_MAX_FILE_SIZE = 209715200;
    File DEFAULT_LOG_DIRECTORY = new File(System.getProperty("user.dir"), "logs");
    boolean DEFAULT_CONSOLE_ENABLED = false;
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    String DEFAULT_LOG_FORMATTER = "com.firefly.utils.log.DefaultLogFormatter";
    String DEFAULT_LOG_NAME_FORMATTER = "com.firefly.utils.log.DefaultLogNameFormatter";
    String DEFAULT_LOG_FILTER = "com.firefly.utils.log.DefaultLogFilter";
    MaxSplitTimeEnum DEFAULT_MAX_SPLIT_TIME = MaxSplitTimeEnum.DAY;

    boolean parse(Action1<FileLog> action);

    FileLog createDefaultLog();

}
