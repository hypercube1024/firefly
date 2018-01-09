package com.firefly.codec.http2.stream;

import com.firefly.net.SecureSessionFactory;
import com.firefly.net.tcp.secure.conscrypt.ConscryptSecureSessionFactory;

public class HTTP2Configuration {

    // TCP settings
    private com.firefly.net.Config tcpConfiguration = new com.firefly.net.Config();

    // SSL/TLS settings
    private boolean isSecureConnectionEnabled;
    private SecureSessionFactory secureSessionFactory = new ConscryptSecureSessionFactory();

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
    private int http2PingInterval = 10 * 1000;
    private int websocketPingInterval = 10 * 1000;

    /**
     * Get the TCP configuration.
     *
     * @return The TCP configuration.
     */
    public com.firefly.net.Config getTcpConfiguration() {
        return tcpConfiguration;
    }

    /**
     * Set the TCP configuration.
     *
     * @param tcpConfiguration The TCP configuration.
     */
    public void setTcpConfiguration(com.firefly.net.Config tcpConfiguration) {
        this.tcpConfiguration = tcpConfiguration;
    }

    /**
     * Get the max dynamic table size of HTTP2 protocol.
     *
     * @return The max dynamic table size of HTTP2 protocol.
     */
    public int getMaxDynamicTableSize() {
        return maxDynamicTableSize;
    }

    /**
     * Set the max dynamic table size of HTTP2 protocol.
     *
     * @param maxDynamicTableSize The max dynamic table size of HTTP2 protocol.
     */
    public void setMaxDynamicTableSize(int maxDynamicTableSize) {
        this.maxDynamicTableSize = maxDynamicTableSize;
    }

    /**
     * Get the HTTP2 stream idle timeout. The time unit is millisecond.
     *
     * @return The HTTP2 stream idle timeout. The time unit is millisecond.
     */
    public int getStreamIdleTimeout() {
        return streamIdleTimeout;
    }

    /**
     * Set the HTTP2 stream idle timeout. The time unit is millisecond.
     *
     * @param streamIdleTimeout The HTTP2 stream idle timeout. The time unit is millisecond.
     */
    public void setStreamIdleTimeout(int streamIdleTimeout) {
        this.streamIdleTimeout = streamIdleTimeout;
    }

    /**
     * Get the HTTP2 flow control strategy. The value is "simple" or "buffer".
     * If you use the "simple" flow control strategy, once the server or client receives the data, it will send the WindowUpdateFrame.
     * If you use the "buffer" flow control strategy, the server or client will send WindowUpdateFrame when the consumed data exceed the threshold.
     *
     * @return The HTTP2 flow control strategy. The value is "simple" or "buffer".
     */
    public String getFlowControlStrategy() {
        return flowControlStrategy;
    }

    /**
     * Set the HTTP2 flow control strategy. The value is "simple" or "buffer".
     * If you use the "simple" flow control strategy, once the server or client receives the data, it will send the WindowUpdateFrame.
     * If you use the "buffer" flow control strategy, the server or client will send WindowUpdateFrame when the consumed data exceed the threshold.
     *
     * @param flowControlStrategy The HTTP2 flow control strategy. The value is "simple" or "buffer".
     */
    public void setFlowControlStrategy(String flowControlStrategy) {
        this.flowControlStrategy = flowControlStrategy;
    }

    /**
     * Get the HTTP2 initial receiving window size. The unit is byte.
     *
     * @return the HTTP2 initial receiving window size. The unit is byte.
     */
    public int getInitialSessionRecvWindow() {
        return initialSessionRecvWindow;
    }

    /**
     * Set the HTTP2 initial receiving window size. The unit is byte.
     *
     * @param initialSessionRecvWindow The HTTP2 initial receiving window size. The unit is byte.
     */
    public void setInitialSessionRecvWindow(int initialSessionRecvWindow) {
        this.initialSessionRecvWindow = initialSessionRecvWindow;
    }

    /**
     * Get the HTTP2 initial sending window size. The unit is byte.
     *
     * @return The HTTP2 initial sending window size. The unit is byte.
     */
    public int getInitialStreamSendWindow() {
        return initialStreamSendWindow;
    }

    /**
     * Set the HTTP2 initial sending window size. The unit is byte.
     *
     * @param initialStreamSendWindow the HTTP2 initial sending window size. The unit is byte.
     */
    public void setInitialStreamSendWindow(int initialStreamSendWindow) {
        this.initialStreamSendWindow = initialStreamSendWindow;
    }

    /**
     * Get the max concurrent stream size in a HTTP2 session.
     *
     * @return the max concurrent stream size in a HTTP2 session.
     */
    public int getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }

    /**
     * Set the max concurrent stream size in a HTTP2 session.
     *
     * @param maxConcurrentStreams the max concurrent stream size in a HTTP2 session.
     */
    public void setMaxConcurrentStreams(int maxConcurrentStreams) {
        this.maxConcurrentStreams = maxConcurrentStreams;
    }

    /**
     * Set the max HTTP2 header block size. If the header block size more the this value,
     * the server or client will split the header buffer to many buffers to send.
     *
     * @return the max HTTP2 header block size.
     */
    public int getMaxHeaderBlockFragment() {
        return maxHeaderBlockFragment;
    }

    /**
     * Get the max HTTP2 header block size. If the header block size more the this value,
     * the server or client will split the header buffer to many buffers to send.
     *
     * @param maxHeaderBlockFragment The max HTTP2 header block size.
     */
    public void setMaxHeaderBlockFragment(int maxHeaderBlockFragment) {
        this.maxHeaderBlockFragment = maxHeaderBlockFragment;
    }

    /**
     * Get the max HTTP request header size.
     *
     * @return the max HTTP request header size.
     */
    public int getMaxRequestHeadLength() {
        return maxRequestHeadLength;
    }

    /**
     * Set the max HTTP request header size.
     *
     * @param maxRequestHeadLength the max HTTP request header size.
     */
    public void setMaxRequestHeadLength(int maxRequestHeadLength) {
        this.maxRequestHeadLength = maxRequestHeadLength;
    }

    /**
     * Get the max HTTP response header size.
     *
     * @return the max HTTP response header size.
     */
    public int getMaxResponseHeadLength() {
        return maxResponseHeadLength;
    }

    /**
     * Set the max HTTP response header size.
     *
     * @param maxResponseHeadLength the max HTTP response header size.
     */
    public void setMaxResponseHeadLength(int maxResponseHeadLength) {
        this.maxResponseHeadLength = maxResponseHeadLength;
    }

    /**
     * Get the max HTTP request trailer size.
     *
     * @return the max HTTP request trailer size.
     */
    public int getMaxRequestTrailerLength() {
        return maxRequestTrailerLength;
    }

    /**
     * Set the max HTTP request trailer size.
     *
     * @param maxRequestTrailerLength the max HTTP request trailer size.
     */
    public void setMaxRequestTrailerLength(int maxRequestTrailerLength) {
        this.maxRequestTrailerLength = maxRequestTrailerLength;
    }

    /**
     * Get the max HTTP response trailer size.
     *
     * @return the max HTTP response trailer size.
     */
    public int getMaxResponseTrailerLength() {
        return maxResponseTrailerLength;
    }

    /**
     * Set the max HTTP response trailer size.
     *
     * @param maxResponseTrailerLength the max HTTP response trailer size.
     */
    public void setMaxResponseTrailerLength(int maxResponseTrailerLength) {
        this.maxResponseTrailerLength = maxResponseTrailerLength;
    }

    /**
     * Get the charset of the text HTTP body.
     *
     * @return the charset of the text HTTP body.
     */
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    /**
     * Set the charset of the text HTTP body.
     *
     * @param characterEncoding the charset of the text HTTP body.
     */
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    /**
     * If return true, the server or client enable the SSL/TLS connection.
     *
     * @return If return true, the server or client enable the SSL/TLS connection.
     */
    public boolean isSecureConnectionEnabled() {
        return isSecureConnectionEnabled;
    }

    /**
     * If set true, the server or client enable the SSL/TLS connection.
     *
     * @param isSecureConnectionEnabled If set true, the server or client enable the SSL/TLS connection.
     */
    public void setSecureConnectionEnabled(boolean isSecureConnectionEnabled) {
        this.isSecureConnectionEnabled = isSecureConnectionEnabled;
    }

    /**
     * Get the SSL/TLS connection factory.
     *
     * @return the SSL/TLS connection factory.
     */
    public SecureSessionFactory getSecureSessionFactory() {
        return secureSessionFactory;
    }

    /**
     * Set the SSL/TLS connection factory.
     *
     * @param secureSessionFactory the SSL/TLS connection factory.
     */
    public void setSecureSessionFactory(SecureSessionFactory secureSessionFactory) {
        this.secureSessionFactory = secureSessionFactory;
    }

    /**
     * Get the default HTTP protocol version. The value is "HTTP/2.0" or "HTTP/1.1". If the value is null,
     * the server or client will negotiate a HTTP protocol version using ALPN.
     *
     * @return the default HTTP protocol version. The value is "HTTP/2.0" or "HTTP/1.1".
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set the default HTTP protocol version. The value is "HTTP/2.0" or "HTTP/1.1". If the value is null,
     * the server or client will negotiate a HTTP protocol version using ALPN.
     *
     * @param protocol the default HTTP protocol version. The value is "HTTP/2.0" or "HTTP/1.1".
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Get the HTTP2 connection sending ping frame interval. The time unit is millisecond.
     *
     * @return the sending ping frame interval. The time unit is millisecond.
     */
    public int getHttp2PingInterval() {
        return http2PingInterval;
    }

    /**
     * Set the sending ping frame interval. The time unit is millisecond.
     *
     * @param http2PingInterval the sending ping frame interval. The time unit is millisecond.
     */
    public void setHttp2PingInterval(int http2PingInterval) {
        this.http2PingInterval = http2PingInterval;
    }

    /**
     * Get the WebSocket connection sending ping frame interval. The time unit is millisecond.
     *
     * @return the WebSocket connection sending ping frame interval. The time unit is millisecond.
     */
    public int getWebsocketPingInterval() {
        return websocketPingInterval;
    }

    /**
     * Set the WebSocket connection sending ping frame interval. The time unit is millisecond.
     *
     * @param websocketPingInterval the WebSocket connection sending ping frame interval. The time unit is millisecond.
     */
    public void setWebsocketPingInterval(int websocketPingInterval) {
        this.websocketPingInterval = websocketPingInterval;
    }
}
