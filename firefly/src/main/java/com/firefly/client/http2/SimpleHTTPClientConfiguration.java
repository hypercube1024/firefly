package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;

public class SimpleHTTPClientConfiguration extends HTTP2Configuration {

	private int initPoolSize = 1;
	private int maxPoolSize = 8;
	private int takeConnectionTimeout = 3 * 1000;
	private int cleanupInterval = 30 * 1000;
	private int cleanupInitialDelay = 15 * 1000;

	public int getInitPoolSize() {
		return initPoolSize;
	}

	public void setInitPoolSize(int initPoolSize) {
		this.initPoolSize = initPoolSize;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public int getTakeConnectionTimeout() {
		return takeConnectionTimeout;
	}

	public void setTakeConnectionTimeout(int takeConnectionTimeout) {
		this.takeConnectionTimeout = takeConnectionTimeout;
	}

	public int getCleanupInterval() {
		return cleanupInterval;
	}

	public void setCleanupInterval(int cleanupInterval) {
		this.cleanupInterval = cleanupInterval;
	}

	public int getCleanupInitialDelay() {
		return cleanupInitialDelay;
	}

	public void setCleanupInitialDelay(int cleanupInitialDelay) {
		this.cleanupInitialDelay = cleanupInitialDelay;
	}

}
