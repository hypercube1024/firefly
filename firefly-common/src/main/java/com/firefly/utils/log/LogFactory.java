package com.firefly.utils.log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.collection.TreeTrie;
import com.firefly.utils.collection.Trie;
import com.firefly.utils.log.file.FileLog;
import com.firefly.utils.log.file.FileLogTask;
import com.firefly.utils.time.Millisecond100Clock;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class LogFactory {
	public static final SafeSimpleDateFormat DAY_DATE_FORMAT = new SafeSimpleDateFormat("yyyy-MM-dd");
	public static final String CONFIGURATION_FILE_NAME = "firefly-log.properties";
	public static final String DEFAULT_LOG_NAME = "firefly-system";
	public static final String DEFAULT_LOG_LEVEL = "INFO";
	public static final File DEFAULT_LOG_DIRECTORY = new File(System.getProperty("user.dir"), "logs");

	private final Trie<Log> logTree = new TreeTrie<>();
	private final LogTask logTask;

	private static class Holder {
		private static LogFactory instance = new LogFactory();
	}

	public static LogFactory getInstance() {
		return Holder.instance;
	}

	private LogFactory() {
		logTask = new FileLogTask(logTree);

		try {
			Properties properties = loadLogConfigurationFile();
			if (properties != null) {
				parseProperties(properties);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (logTree.get(DEFAULT_LOG_NAME) == null) {
			createDefaultLog();
		}

		logTask.start();
	}

	private Properties loadLogConfigurationFile() throws IOException {
		Properties properties = new Properties();
		try (InputStream input = LogFactory.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE_NAME)) {
			if (input == null)
				return null;
			properties.load(input);
		}
		return properties;
	}

	private boolean createLogDirectory(File file) {
		if (file.exists() && file.isDirectory()) {
			return true;
		} else {
			return file.mkdirs();
		}
	}

	private void createDefaultLog() {
		createLog(DEFAULT_LOG_NAME, DEFAULT_LOG_LEVEL, null, false);
	}

	private void createLog(String name, String level, String path, boolean console) {
		FileLog fileLog = new FileLog();
		fileLog.setName(name);
		fileLog.setLevel(LogLevel.fromName(level));

		boolean createLogDirectorySuccess = false;
		if (VerifyUtils.isNotEmpty(path)) {
			File file = new File(path);
			createLogDirectorySuccess = createLogDirectory(file);
			if (createLogDirectorySuccess) {
				fileLog.setPath(path);
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
			fileLog.setConsoleOutput(console);
		} else {
			fileLog.setConsoleOutput(true);
			System.err.println("create log directory is failure");
		}

		logTree.put(name, fileLog);
		System.out.println("initialize log " + fileLog.toString());
	}

	private void parseProperties(Properties properties) {
		for (Entry<Object, Object> entry : properties.entrySet()) {
			String name = (String) entry.getKey();
			String value = (String) entry.getValue();

			String[] strs = StringUtils.split(value, ',');

			switch (strs.length) {
			case 1:
				createLog(name, strs[0], null, false);
				break;
			case 2:
				if ("console".equalsIgnoreCase(strs[1])) {
					createLog(name, strs[0], null, true);
				} else {
					createLog(name, strs[0], strs[1], false);
				}
				break;
			case 3:
				createLog(name, strs[0], strs[1], "console".equalsIgnoreCase(strs[2]));
				break;
			default:
				System.err.println(
						"The log " + name + " configuration format is illegal. It will use default log configuration");
				createLog(name, DEFAULT_LOG_LEVEL, null, false);
				break;
			}
		}
	}

	public void flushAll() {
		for (String key : logTree.keySet()) {
			Log log = logTree.get(key);
			if (log instanceof FileLog) {
				((FileLog) log).flush();
			}
		}
	}
	
	public Log getLog(Class<?> clazz) {
		return getLog(clazz.getName());
	}

	public Log getLog(String name) {
		Log log = logTree.getBest(name);
		if (log != null) {
			return log;
		} else {
			return logTree.get(DEFAULT_LOG_NAME);
		}
	}

	public LogTask getLogTask() {
		return logTask;
	}

	public void shutdown() {
		logTask.shutdown();
		Millisecond100Clock.stop();
	}

	public void start() {
		logTask.start();
	}

}
