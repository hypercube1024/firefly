package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;

public class SimpleHTTPClientConfiguration extends HTTP2Configuration {

	private int initPoolSize = 4;
	private int maxPoolSize = 16;
	private int takeConnectionTimeout = 2 * 1000;

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

}
