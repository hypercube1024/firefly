package com.firefly.utils.log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.firefly.utils.StringUtils;
import com.firefly.utils.log.file.FileLog;
import com.firefly.utils.log.file.FileLogTask;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class LogFactory {
	public static final SafeSimpleDateFormat DAY_DATE_FORMAT = new SafeSimpleDateFormat("yyyy-MM-dd");
	public static final String CONFIGURATION_FILE_NAME = "firefly-log.properties";
	public static final String DEFAULT_LOG_NAME = "firefly-system";
	
	private final Map<String, Log> logMap;
	private final LogTask logTask;

	private static class Holder {
		private static LogFactory instance = new LogFactory();
	}

	public static LogFactory getInstance() {
		return Holder.instance;
	}

	private LogFactory() {
		logMap = new HashMap<String, Log>();
		logTask = new FileLogTask(logMap);

		createDefaultLog();
		
		try {
			Properties properties = loadLogConfigurationFile();
			parseProperties(properties);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logTask.start();
	}

	private void createDefaultLog() {
		FileLog fileLog = new FileLog();
		fileLog.setName(DEFAULT_LOG_NAME);
		fileLog.setLevel(LogLevel.INFO);
		
		boolean setLogDirectorySuccess = false;
		File logDir = new File(System.getProperty("user.dir"), "logs");
		if(logDir.isDirectory() && logDir.exists()) {
			fileLog.setPath(logDir.getAbsolutePath());
			setLogDirectorySuccess = true;
		} else {
			boolean success = logDir.mkdirs();
			if(success) {
				fileLog.setPath(logDir.getAbsolutePath());
				setLogDirectorySuccess = true;
			} else {
				setLogDirectorySuccess = false;
			}
		}
		
		if(setLogDirectorySuccess) {
			fileLog.setFileOutput(true);
			fileLog.setConsoleOutput(false);
		} else {
			fileLog.setFileOutput(false);
			fileLog.setConsoleOutput(true);
		}
		
		logMap.put(DEFAULT_LOG_NAME, fileLog);
		System.out.println("create default log: " + fileLog.toString());
	}
	
	private Properties loadLogConfigurationFile() throws IOException{
		Properties properties = new Properties();
		try (InputStream input = LogFactory.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE_NAME)) {
			properties.load(input);
		}
		return properties;
	}

	private void parseProperties(Properties properties) {
		for (Entry<Object, Object> entry : properties.entrySet()) {
			String name = (String) entry.getKey();
			String value = (String) entry.getValue();
//			System.out.println(name + "|" + value);

			String[] strs = StringUtils.split(value, ',');
			if (strs.length < 2) {
				System.err.println("the log configuration file format is illegal");
				continue;
			}

			String path = strs[1];
			FileLog fileLog = new FileLog();
			fileLog.setName(name);
			fileLog.setLevel(LogLevel.fromName(strs[0]));

			if ("console".equalsIgnoreCase(path)) {
				fileLog.setFileOutput(false);
				fileLog.setConsoleOutput(true);
			} else {
				File file = new File(path);
				if(file.exists() && file.isDirectory()) {
					fileLog.setPath(path);
					fileLog.setFileOutput(true);
				} else {
					boolean success = file.mkdirs();
					if(success) {
						fileLog.setPath(path);
						fileLog.setFileOutput(true);
					} else {
						System.err.println("create directory " + path + " failure");
						continue;
					}
				}

				if (strs.length > 2)
					fileLog.setConsoleOutput("console".equalsIgnoreCase(strs[2]));
			}
			logMap.put(name, fileLog);
			System.out.println("initialize log " + fileLog.toString() + " success");
		}
	}

	public void flush() {
		for (Entry<String, Log> entry : logMap.entrySet()) {
			Log log = entry.getValue();
			if (log instanceof FileLog) {
				try {
					((FileLog)log).flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public Log getLog(String name) {
		return logMap.get(name);
	}

	public LogTask getLogTask() {
		return logTask;
	}

	public void shutdown() {
		logTask.shutdown();
	}

	public void start() {
		logTask.start();
	}

}
