package com.firefly.utils.log;

public interface Log {

	String CL = "\r\n";
	
	boolean isTraceEnable();

	void trace(String str);
	
	void trace(String str, Object... objs);

	void trace(String str, Throwable throwable, Object... objs);
	
	boolean isDebugEnable();

	void debug(String str);
	
	void debug(String str, Object... objs);

	void debug(String str, Throwable throwable, Object... objs);
	
	boolean isInfoEnable();

	void info(String str);
	
	void info(String str, Object... objs);

	void info(String str, Throwable throwable, Object... objs);
	
	boolean isWarnEnable();

	void warn(String str);
	
	void warn(String str, Object... objs);

	void warn(String str, Throwable throwable, Object... objs);
	
	boolean isErrorEnable();

	void error(String str);
	
	void error(String str, Object... objs);

	void error(String str, Throwable throwable, Object... objs);
}
