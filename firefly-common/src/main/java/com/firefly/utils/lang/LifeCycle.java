package com.firefly.utils.lang;

public interface LifeCycle {
	public void start();

	public void stop();

	public boolean isStarted();

	public boolean isStopped();
}
