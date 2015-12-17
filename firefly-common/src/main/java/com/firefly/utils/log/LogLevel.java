package com.firefly.utils.log;

import java.util.HashMap;
import java.util.Map;

public enum LogLevel {

	TRACE(0, "TRACE"), DEBUG(1, "DEBUG"), INFO(2, "INFO"), WARN(3, "WARN"), ERROR(4, "ERROR");

	private final int level;
	private final String name;
	private static final LogLevel[] levels = new LogLevel[5];
	private static final Map<String, LogLevel> levelNameMap = new HashMap<>();

	static {
		for (LogLevel logLevel : LogLevel.values()) {
			levels[logLevel.level] = logLevel;
			levelNameMap.put(logLevel.name, logLevel);
		}
	}

	private LogLevel(int level, String name) {
		this.level = level;
		this.name = name;
	}

	public int getLevel() {
		return level;
	}

	public String getName() {
		return name;
	}
	
	public boolean isEnabled(LogLevel logLevel) {
		return this.level <= logLevel.level;
	}

	public static LogLevel fromLevel(int level) {
		return levels[level];
	}

	public static LogLevel fromName(String name) {
		return levelNameMap.get(name);
	}
	
}
