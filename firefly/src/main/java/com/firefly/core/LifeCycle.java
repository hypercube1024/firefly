package com.firefly.core;

public interface LifeCycle {
	public void start();

	public void stop();

	public boolean isStarted();

	public boolean isStopped();
}
