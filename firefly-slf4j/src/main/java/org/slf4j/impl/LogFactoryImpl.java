package org.slf4j.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class LogFactoryImpl implements ILoggerFactory {

	private Map<String, Logger> map = new ConcurrentHashMap<>();

	@Override
	public Logger getLogger(String name) {
		Logger logger = map.get(name);
		if (logger != null) {
			return logger;
		} else {
			Log log = LogFactory.getInstance().getLog(name);
			if (log != null) {
				Logger newInstance = new LoggerImpl(log);
				Logger oldInstance = map.putIfAbsent(name, newInstance);
				return oldInstance == null ? newInstance : oldInstance;
			} else {
				return null;
			}
		}
	}

}
