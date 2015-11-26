package com.firefly.core;

public abstract class AbstractLifeCycle implements LifeCycle {
	
	protected volatile boolean start;

	@Override
	public boolean isStarted() {
		return start;
	}

	@Override
	public boolean isStopped() {
		return !start;
	}

}
