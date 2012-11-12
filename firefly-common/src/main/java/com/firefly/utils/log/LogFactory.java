package com.firefly.utils.log;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import com.firefly.utils.StringUtils;
import com.firefly.utils.log.file.FileLog;
import com.firefly.utils.log.file.FileLogTask;
import com.firefly.utils.log.slf4j.Slf4jLog;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class LogFactory {
	private Map<String, Log> logMap = new HashMap<String, Log>();
	private Map<String, Integer> levelMap = new HashMap<String, Integer>();
	public static final SafeSimpleDateFormat dayDateFormat = new SafeSimpleDateFormat("yyyy-MM-dd");
	private LogTask logTask = new FileLogTask();
	private boolean logbackCanWork = Slf4jLog.canWork();

	private static class Holder {
		private static LogFactory instance = new LogFactory();
	}

	public static LogFactory getInstance() {
		return Holder.instance;
	}

	private LogFactory() {
		// 优先使用slf4j，如果类路径下没有slf4j包再初始化firefly框架的日志系统
		if (!logbackCanWork) {
			levelMap.put("TRACE", Log.TRACE);
			levelMap.put("DEBUG", Log.DEBUG);
			levelMap.put("INFO", Log.INFO);
			levelMap.put("WARN", Log.WARN);
			levelMap.put("ERROR", Log.ERROR);
			defaultLog();

			File configFile = null;
			try {
				configFile = new File(LogFactory.class.getClassLoader().getResource("firefly-log.properties").toURI());
			}
			catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
			if (configFile != null && configFile.exists()) {
				loadProperties();
			}
			logTask.start();
		}
	}

	private void defaultLog() {
		String name = "firefly-system";
		FileLog fileLog = new FileLog();
		fileLog.setName(name);
		fileLog.setLevel(2);
		fileLog.setFileOutput(false);
		fileLog.setConsoleOutput(true);
		System.out.println(name + "|console");
		logMap.put(name, fileLog);
	}

	private void loadProperties() {
		Properties properties = new Properties();
		try {
			properties.load(LogFactory.class.getClassLoader().getResourceAsStream("firefly-log.properties"));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		for (Entry<Object, Object> entry : properties.entrySet()) {
			String name = (String) entry.getKey();
			String value = (String) entry.getValue();
			System.out.println(name + "|" + value);

			String[] strs = StringUtils.split(value, ',');
			if (strs.length < 2)
				throw new LogException("config format error! example: firefly-system=DEBUG,console");

			int level = levelMap.get(strs[0]);
			String path = strs[1];
			FileLog fileLog = new FileLog();
			fileLog.setName(name);
			fileLog.setLevel(level);

			if ("console".equalsIgnoreCase(path)) {
				fileLog.setFileOutput(false);
				fileLog.setConsoleOutput(true);
			} else {
				File file = new File(path);
				if (!file.exists()) {
					boolean mkdirRet = file.mkdir();
					if (!mkdirRet)
						throw new LogException("create dir " + path + " failure");
				}

				if (!file.isDirectory())
					throw new LogException(path + " is not directory");

				fileLog.setPath(path);
				fileLog.setFileOutput(true);
				if (strs.length > 2)
					fileLog.setConsoleOutput("console".equalsIgnoreCase(strs[2]));
			}

			logMap.put(name, fileLog);
		}
	}

	public void flush() {
		if (!logbackCanWork)
			for (Entry<String, Log> entry : logMap.entrySet()) {
				Log log = entry.getValue();
				if (log instanceof FileLog) {

					FileLog fileLog = (FileLog) log;
					fileLog.flush();
					// System.out.println(">>> flush all " + fileLog.getName());
				}
			}
	}

	public Log getLog(String name) {
		if (logMap.containsKey(name))
			return logMap.get(name);
		else {
			if (logbackCanWork) {
				Log log = new Slf4jLog(name);
				logMap.put("name", log);
				return log;
			}
			// 如果没有配置过该名称的日志，则缺省使用 defaultLog 输出
			return logMap.get("firefly-system");
		}
	}

	public LogTask getLogTask() {
		return logTask;
	}

	public void shutdown() {
		if (!logbackCanWork)
			logTask.shutdown();
	}

	public void start() {
		if (!logbackCanWork)
			logTask.start();
	}

}
