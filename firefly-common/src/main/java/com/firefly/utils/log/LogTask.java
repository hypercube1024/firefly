package com.firefly.utils.log;

public interface LogTask extends Runnable {
	void start();

	void shutdown();

	void add(LogItem logItem);
}
