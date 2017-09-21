package com.firefly.codec.http2.stream;

import com.firefly.net.SSLContextFactory;
import com.firefly.net.tcp.ssl.DefaultJavaSSLContextFactory;
import com.firefly.net.tcp.ssl.SelfSignedCertificateOpenSSLContextFactory;

public class HTTP2Configuration {

    // TCP settings
    private com.firefly.net.Config tcpConfiguration = new com.firefly.net.Config();

    // SSL/TLS settings
    private boolean isSecureConnectionEnabled;
    private SSLContextFactory sslContextFactory = new DefaultJavaSSLContextFactory();

    // HTTP settings
    private int maxDynamicTableSize = 4096;
    private int streamIdleTimeout = 10 * 1000;
    private String flowControlStrategy = "buffer";
    private int initialStreamSendWindow = FlowControlStrategy.DEFAULT_WINDOW_SIZE;
    private int initialSessionRecvWindow = FlowControlStrategy.DEFAULT_WINDOW_SIZE;
    private int maxConcurrentStreams = -1;
    private int maxHeaderBlockFragment = 0;
    private int maxRequestHeadLength = 4 * 1024;
    private int maxRequestTrailerLength = 4 * 1024;
    private int maxResponseHeadLength = 4 * 1024;
    private int maxResponseTrailerLength = 4 * 1024;
    private String characterEncoding = "UTF-8";
    private String protocol; // HTTP/2.0, HTTP/1.1

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

    public int getMaxRequestTrailerLength() {
        return maxRequestTrailerLength;
    }

    public void setMaxRequestTrailerLength(int maxRequestTrailerLength) {
        this.maxRequestTrailerLength = maxRequestTrailerLength;
    }

    public int getMaxResponseTrailerLength() {
        return maxResponseTrailerLength;
    }

    public void setMaxResponseTrailerLength(int maxResponseTrailerLength) {
        this.maxResponseTrailerLength = maxResponseTrailerLength;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
