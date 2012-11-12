package com.firefly.utils.log.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.firefly.utils.StringUtils;
import com.firefly.utils.log.Log;

/**
 * slf4j 日志实现
 * <p/>
 * 系统的类加载路径下存在<code>org.slf4j.LoggerFactory</code>就使用slf4j框架输出日志.
 * 
 * @author Gongqin(QinerG@gmail.com)
 */
public class Slf4jLog implements Log {

	private Logger logger;

	public Slf4jLog(String className) {
		logger = LoggerFactory.getLogger(className);
	}
	
	/**
	 * Logback 日志框架是否可以使用
	 * @return
	 */
	public static boolean canWork() {
		try {
			Class.forName("org.slf4j.LoggerFactory", false, Slf4jLog.class.getClassLoader());
			return true;
		}
		catch (Throwable e) {}
		return false;
	}

	@Override
	public boolean isTraceEnable() {
		return logger.isTraceEnabled();
	}

	@Override
	public void trace(String str) {
		if (isTraceEnable())
			logger.trace(str);
	}

	@Override
	public void trace(String str, Object... objs) {
		if (isTraceEnable())
			logger.trace(str, objs);
	}

	@Override
	public void trace(String str, Throwable throwable, Object... objs) {
		if (isTraceEnable())
			logger.trace(StringUtils.replace(str, objs), throwable);
	}

	@Override
	public boolean isDebugEnable() {
		return logger.isDebugEnabled();
	}

	@Override
	public void debug(String str) {
		if (isDebugEnable())
			logger.debug(str);
	}

	@Override
	public void debug(String str, Object... objs) {
		if (isDebugEnable())
			logger.debug(str, objs);
	}

	@Override
	public void debug(String str, Throwable throwable, Object... objs) {
		if (isDebugEnable())
			logger.debug(StringUtils.replace(str, objs), throwable);
	}

	@Override
	public boolean isInfoEnable() {
		return logger.isInfoEnabled();
	}

	@Override
	public void info(String str) {
		if (isInfoEnable())
			logger.info(str);
	}

	@Override
	public void info(String str, Object... objs) {
		if (isInfoEnable())
			logger.info(str, objs);
	}

	@Override
	public void info(String str, Throwable throwable, Object... objs) {
		if (isInfoEnable())
			logger.info(StringUtils.replace(str, objs), throwable);
	}

	@Override
	public boolean isWarnEnable() {
		return logger.isWarnEnabled();
	}

	@Override
	public void warn(String str) {
		if (isWarnEnable())
			logger.warn(str);
	}

	@Override
	public void warn(String str, Object... objs) {
		if (isWarnEnable())
			logger.warn(str, objs);
	}

	@Override
	public void warn(String str, Throwable throwable, Object... objs) {
		if (isWarnEnable())
			logger.warn(StringUtils.replace(str, objs), throwable);
	}

	@Override
	public boolean isErrorEnable() {
		return logger.isErrorEnabled();
	}

	@Override
	public void error(String str) {
		if (isErrorEnable())
			logger.error(str);
	}

	@Override
	public void error(String str, Object... objs) {
		if (isErrorEnable())
			logger.error(str, objs);
	}

	@Override
	public void error(String str, Throwable throwable, Object... objs) {
		if (isErrorEnable())
			logger.error(StringUtils.replace(str, objs), throwable);
	}
}
