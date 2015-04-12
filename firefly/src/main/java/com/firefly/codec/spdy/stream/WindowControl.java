package com.firefly.codec.spdy.stream;

import java.util.concurrent.atomic.AtomicInteger;

public class WindowControl {
	public static final int DEFAULT_INITIALIZED_WINDOW_SIZE = 64 * 1024;
	
	private final AtomicInteger windowSize = new AtomicInteger(DEFAULT_INITIALIZED_WINDOW_SIZE);
	
	public WindowControl() {}
	
	public WindowControl(int initWindowSize) {
		windowSize.set(initWindowSize);
	}
	
	public int reduceWindowSize(int delta) {
		return windowSize.addAndGet(-delta);
	}
	
	public int addWindowSize(int delta) {
		return windowSize.addAndGet(delta);
	}
	
	public int windowSize() {
		return windowSize.get();
	}
	
	public void setWindowSize(int size) {
		windowSize.set(size);
	}
}
