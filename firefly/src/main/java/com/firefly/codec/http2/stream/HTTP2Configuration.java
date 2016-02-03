package com.firefly.codec.http2.stream;

import java.io.File;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.FileAccessFilter;
import com.firefly.net.SSLContextFactory;
import com.firefly.net.tcp.ssl.DefaultCredentialSSLContextFactory;
import com.firefly.server.http2.servlet.session.HttpSessionManager;
import com.firefly.server.http2.servlet.session.LocalHttpSessionManager;

public class HTTP2Configuration {
	
	public static final String DEFAULT_CONFIG_FILE_NAME = "firefly.xml";

	// TCP settings
	private com.firefly.net.Config tcpConfiguration = new com.firefly.net.Config();

	// SSL/TLS settings
	private boolean isSecureConnectionEnabled;
	private SSLContextFactory sslContextFactory = new DefaultCredentialSSLContextFactory();

	// HTTP settings
	private int maxDynamicTableSize = 4096;
	private int streamIdleTimeout = 10 * 1000;
	private String flowControlStrategy = "buffer";
	private int initialStreamSendWindow = FlowControlStrategy.DEFAULT_WINDOW_SIZE;
	private int initialSessionRecvWindow = FlowControlStrategy.DEFAULT_WINDOW_SIZE;
	private int maxConcurrentStreams = -1;
	private int maxHeaderBlockFragment = 0;
	private int maxRequestHeadLength = 4 * 1024;
	private int maxResponseHeadLength = 4 * 1024;
	private String characterEncoding = "UTF-8";

	// servlet server settings
	private int httpBodyThreshold = 4 * 1024 * 1024;
	private String temporaryDirectory = new File(System.getProperty("user.dir"), "temp").getAbsolutePath();
	private int servletResponseBufferSize = 8 * 1024;
	private String configFileName = DEFAULT_CONFIG_FILE_NAME;
	private String serverHome;
	private String host;
	private int port;
	private int maxRangeNum = 8;
	private Map<Integer, String> errorPage;
	private FileAccessFilter fileAccessFilter = new FileAccessFilter() {
		@Override
		public String doFilter(HttpServletRequest request, HttpServletResponse response, String path) {
			return path;
		}
	};

	// servlet session settings
	private String sessionIdName = "jsessionid";
	private HttpSessionManager httpSessionManager = new LocalHttpSessionManager();

	// asynchronous context pool settings
	private int asynchronousContextCorePoolSize = Runtime.getRuntime().availableProcessors();
	private int asynchronousContextMaximumPoolSize = 64;
	private int asynchronousContextCorePoolKeepAliveTime = 10 * 1000;
	private int asynchronousContextTimeout = 6 * 1000;

	public com.firefly.net.Config getTcpConfiguration() {
		return tcpConfiguration;
	}

	public void setTcpConfiguration(com.firefly.net.Config tcpConfiguration) {
		this.tcpConfiguration = tcpConfiguration;
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

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	public String getTemporaryDirectory() {
		return temporaryDirectory;
	}

	public void setTemporaryDirectory(String temporaryDirectory) {
		this.temporaryDirectory = temporaryDirectory;
	}

	public int getHttpBodyThreshold() {
		return httpBodyThreshold;
	}

	public void setHttpBodyThreshold(int httpBodyThreshold) {
		this.httpBodyThreshold = httpBodyThreshold;
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

	public String getSessionIdName() {
		return sessionIdName;
	}

	public void setSessionIdName(String sessionIdName) {
		this.sessionIdName = sessionIdName;
	}

	public HttpSessionManager getHttpSessionManager() {
		return httpSessionManager;
	}

	public void setHttpSessionManager(HttpSessionManager httpSessionManager) {
		this.httpSessionManager = httpSessionManager;
	}

	public int getAsynchronousContextCorePoolSize() {
		return asynchronousContextCorePoolSize;
	}

	public void setAsynchronousContextCorePoolSize(int asynchronousContextCorePoolSize) {
		this.asynchronousContextCorePoolSize = asynchronousContextCorePoolSize;
	}

	public int getAsynchronousContextMaximumPoolSize() {
		return asynchronousContextMaximumPoolSize;
	}

	public void setAsynchronousContextMaximumPoolSize(int asynchronousContextMaximumPoolSize) {
		this.asynchronousContextMaximumPoolSize = asynchronousContextMaximumPoolSize;
	}

	public int getAsynchronousContextCorePoolKeepAliveTime() {
		return asynchronousContextCorePoolKeepAliveTime;
	}

	public void setAsynchronousContextCorePoolKeepAliveTime(int asynchronousContextCorePoolKeepAliveTime) {
		this.asynchronousContextCorePoolKeepAliveTime = asynchronousContextCorePoolKeepAliveTime;
	}

	public int getAsynchronousContextTimeout() {
		return asynchronousContextTimeout;
	}

	public void setAsynchronousContextTimeout(int asynchronousContextTimeout) {
		this.asynchronousContextTimeout = asynchronousContextTimeout;
	}

	public int getServletResponseBufferSize() {
		return servletResponseBufferSize;
	}

	public void setServletResponseBufferSize(int servletResponseBufferSize) {
		this.servletResponseBufferSize = servletResponseBufferSize;
	}

	public String getServerHome() {
		return serverHome;
	}

	public void setServerHome(String serverHome) {
		this.serverHome = serverHome;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getConfigFileName() {
		return configFileName;
	}

	public void setConfigFileName(String configFileName) {
		this.configFileName = configFileName;
	}

	public FileAccessFilter getFileAccessFilter() {
		return fileAccessFilter;
	}

	public void setFileAccessFilter(FileAccessFilter fileAccessFilter) {
		this.fileAccessFilter = fileAccessFilter;
	}

	public int getMaxRangeNum() {
		return maxRangeNum;
	}

	public void setMaxRangeNum(int maxRangeNum) {
		this.maxRangeNum = maxRangeNum;
	}

	public Map<Integer, String> getErrorPage() {
		return errorPage;
	}

	public void setErrorPage(Map<Integer, String> errorPage) {
		this.errorPage = errorPage;
	}

	@Override
	public String toString() {
		return "HTTP2Configuration [tcpConfiguration=" + tcpConfiguration + ", isSecureConnectionEnabled="
				+ isSecureConnectionEnabled + ", sslContextFactory=" + sslContextFactory + ", maxDynamicTableSize="
				+ maxDynamicTableSize + ", streamIdleTimeout=" + streamIdleTimeout + ", flowControlStrategy="
				+ flowControlStrategy + ", initialStreamSendWindow=" + initialStreamSendWindow
				+ ", initialSessionRecvWindow=" + initialSessionRecvWindow + ", maxConcurrentStreams="
				+ maxConcurrentStreams + ", maxHeaderBlockFragment=" + maxHeaderBlockFragment
				+ ", maxRequestHeadLength=" + maxRequestHeadLength + ", maxResponseHeadLength=" + maxResponseHeadLength
				+ ", characterEncoding=" + characterEncoding + ", httpBodyThreshold=" + httpBodyThreshold
				+ ", temporaryDirectory=" + temporaryDirectory + ", servletResponseBufferSize="
				+ servletResponseBufferSize + ", configFileName=" + configFileName + ", serverHome=" + serverHome
				+ ", host=" + host + ", port=" + port + ", maxRangeNum=" + maxRangeNum + ", errorPage=" + errorPage
				+ ", fileAccessFilter=" + fileAccessFilter + ", sessionIdName=" + sessionIdName
				+ ", httpSessionManager=" + httpSessionManager + ", asynchronousContextCorePoolSize="
				+ asynchronousContextCorePoolSize + ", asynchronousContextMaximumPoolSize="
				+ asynchronousContextMaximumPoolSize + ", asynchronousContextCorePoolKeepAliveTime="
				+ asynchronousContextCorePoolKeepAliveTime + ", asynchronousContextTimeout="
				+ asynchronousContextTimeout + "]";
	}

}
