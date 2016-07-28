package com.firefly.utils.log;

import java.io.File;

import com.firefly.utils.function.Action1;
import com.firefly.utils.log.file.FileLog;

public interface LogConfigParser {

	public static final String DEFAULT_XML_CONFIG_FILE_NAME = "firefly-log.xml";
	public static final String DEFAULT_PROPERTIES_CONFIG_FILE_NAME = "firefly-log.properties";
	public static final String DEFAULT_LOG_NAME = "firefly-system";
	public static final String DEFAULT_LOG_LEVEL = "INFO";
	public static final int DEFAULT_MAX_FILE_SIZE = 209715200;
	public static final File DEFAULT_LOG_DIRECTORY = new File(System.getProperty("user.dir"), "logs");
	public static final boolean DEFAULT_CONSOLE_ENABLED = false;

	public boolean parse(Action1<FileLog> action);

	public FileLog createDefaultLog();

}
