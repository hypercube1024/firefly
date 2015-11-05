package com.firefly.server.http2;

import com.firefly.codec.http2.stream.FlowControlStrategy;

public class HTTP2Configuration {

	private int maxDynamicTableSize = 4096;
	private int streamIdleTimeout;
	private String flowControlStrategy = "buffer";
	private int initialStreamSendWindow = FlowControlStrategy.DEFAULT_WINDOW_SIZE;
	private int maxConcurrentStreams = -1;
    private int maxHeaderBlockFragment = 0;
    private int requestHeaderSize = 8*1024;

	public int getMaxDynamicTableSize() {
		return maxDynamicTableSize;
	}

	public void setMaxDynamicTableSize(int maxDynamicTableSize) {
		this.maxDynamicTableSize = maxDynamicTableSize;
	}

	public int getStreamIdleTimeout() {
		return streamIdleTimeout;
	}

	public void setStreamIdleTimeout(int streamIdleTimeout) {
		this.streamIdleTimeout = streamIdleTimeout;
	}

	public String getFlowControlStrategy() {
		return flowControlStrategy;
	}

	public void setFlowControlStrategy(String flowControlStrategy) {
		this.flowControlStrategy = flowControlStrategy;
	}

	public int getInitialStreamSendWindow() {
		return initialStreamSendWindow;
	}

	public void setInitialStreamSendWindow(int initialStreamSendWindow) {
		this.initialStreamSendWindow = initialStreamSendWindow;
	}

	public int getMaxConcurrentStreams() {
		return maxConcurrentStreams;
	}

	public void setMaxConcurrentStreams(int maxConcurrentStreams) {
		this.maxConcurrentStreams = maxConcurrentStreams;
	}

	public int getMaxHeaderBlockFragment() {
		return maxHeaderBlockFragment;
	}

	public void setMaxHeaderBlockFragment(int maxHeaderBlockFragment) {
		this.maxHeaderBlockFragment = maxHeaderBlockFragment;
	}

	public int getRequestHeaderSize() {
		return requestHeaderSize;
	}

	public void setRequestHeaderSize(int requestHeaderSize) {
		this.requestHeaderSize = requestHeaderSize;
	}

}
