package com.firefly.codec.http2.stream;

import com.firefly.net.SSLContextFactory;
import com.firefly.net.tcp.ssl.DefaultCredentialSSLContextFactory;

public class HTTP2Configuration {

	// TCP settings
	private int tcpIdleTimeout = 10 * 1000;

	// SSL/TLS settings
	private boolean isSecureConnectionEnabled;
	private SSLContextFactory sslContextFactory = new DefaultCredentialSSLContextFactory();

	// HTTP2 settings
	private int maxDynamicTableSize = 4096;
	private int streamIdleTimeout = 10 * 1000;
	private String flowControlStrategy = "buffer";
	private int initialStreamSendWindow = FlowControlStrategy.DEFAULT_WINDOW_SIZE;
	private int initialSessionRecvWindow = FlowControlStrategy.DEFAULT_WINDOW_SIZE;
	private int maxConcurrentStreams = -1;
	private int maxHeaderBlockFragment = 0;

	// common settings
	private int maxRequestHeadLength = 4 * 1024;
	private int maxResponseHeadLength = 4 * 1024;

	public int getTcpIdleTimeout() {
		return tcpIdleTimeout;
	}

	public void setTcpIdleTimeout(int tcpIdleTimeout) {
		this.tcpIdleTimeout = tcpIdleTimeout;
	}

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

	public int getInitialSessionRecvWindow() {
		return initialSessionRecvWindow;
	}

	public void setInitialSessionRecvWindow(int initialSessionRecvWindow) {
		this.initialSessionRecvWindow = initialSessionRecvWindow;
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

	public int getMaxRequestHeadLength() {
		return maxRequestHeadLength;
	}

	public void setMaxRequestHeadLength(int maxRequestHeadLength) {
		this.maxRequestHeadLength = maxRequestHeadLength;
	}

	public int getMaxResponseHeadLength() {
		return maxResponseHeadLength;
	}

	public void setMaxResponseHeadLength(int maxResponseHeadLength) {
		this.maxResponseHeadLength = maxResponseHeadLength;
	}

	public boolean isSecureConnectionEnabled() {
		return isSecureConnectionEnabled;
	}

	public void setSecureConnectionEnabled(boolean isSecureConnectionEnabled) {
		this.isSecureConnectionEnabled = isSecureConnectionEnabled;
	}

	public SSLContextFactory getSslContextFactory() {
		return sslContextFactory;
	}

	public void setSslContextFactory(SSLContextFactory sslContextFactory) {
		this.sslContextFactory = sslContextFactory;
	}

}
