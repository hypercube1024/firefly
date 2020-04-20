package com.fireflysource.net.http.common;

import com.fireflysource.common.coroutine.CoroutineDispatchers;
import com.fireflysource.net.tcp.TcpChannelGroup;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;

public class HttpConfig {

    public static int DEFAULT_WINDOW_SIZE = 65535;
    public static int DEFAULT_HEADER_TABLE_SIZE = 4096;

    private TcpChannelGroup tcpChannelGroup;
    private boolean stopTcpChannelGroup = true;
    private long timeout = 30;
    private int connectionPoolSize = CoroutineDispatchers.INSTANCE.getDefaultPoolSize();
    private long leakDetectorInterval = 60;
    private long releaseTimeout = 60;
    private int headerBufferSize = 4 * 1024;
    private int contentBufferSize = 16 * 1024;
    private SecureEngineFactory secureEngineFactory;
    private int maxDynamicTableSize = DEFAULT_HEADER_TABLE_SIZE;
    private int maxHeaderSize = 32 * 1024;
    private int maxHeaderBlockFragment = 0;
    private int initialStreamRecvWindow = 8 * 1024 * 1024;
    private int maxConcurrentStreams = -1;
    private int initialSessionRecvWindow = 16 * 1024 * 1024;
    private long streamIdleTimeout = 0;
    private long maxUploadFileSize = 200 * 1024 * 1024;
    private long maxRequestBodySize = (4 * 1024) + (200 * 1024 * 1024);
    private int uploadFileSizeThreshold = 4 * 1024 * 1024;

    public TcpChannelGroup getTcpChannelGroup() {
        return tcpChannelGroup;
    }

    public void setTcpChannelGroup(TcpChannelGroup tcpChannelGroup) {
        this.tcpChannelGroup = tcpChannelGroup;
    }

    public boolean isStopTcpChannelGroup() {
        return stopTcpChannelGroup;
    }

    public void setStopTcpChannelGroup(boolean stopTcpChannelGroup) {
        this.stopTcpChannelGroup = stopTcpChannelGroup;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public long getLeakDetectorInterval() {
        return leakDetectorInterval;
    }

    public void setLeakDetectorInterval(long leakDetectorInterval) {
        this.leakDetectorInterval = leakDetectorInterval;
    }

    public long getReleaseTimeout() {
        return releaseTimeout;
    }

    public void setReleaseTimeout(long releaseTimeout) {
        this.releaseTimeout = releaseTimeout;
    }

    public int getHeaderBufferSize() {
        return headerBufferSize;
    }

    public void setHeaderBufferSize(int headerBufferSize) {
        this.headerBufferSize = headerBufferSize;
    }

    public int getContentBufferSize() {
        return contentBufferSize;
    }

    public void setContentBufferSize(int contentBufferSize) {
        this.contentBufferSize = contentBufferSize;
    }

    public SecureEngineFactory getSecureEngineFactory() {
        return secureEngineFactory;
    }

    public void setSecureEngineFactory(SecureEngineFactory secureEngineFactory) {
        this.secureEngineFactory = secureEngineFactory;
    }

    public int getMaxDynamicTableSize() {
        return maxDynamicTableSize;
    }

    public void setMaxDynamicTableSize(int maxDynamicTableSize) {
        this.maxDynamicTableSize = maxDynamicTableSize;
    }

    public int getMaxHeaderSize() {
        return maxHeaderSize;
    }

    public void setMaxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public int getMaxHeaderBlockFragment() {
        return maxHeaderBlockFragment;
    }

    public void setMaxHeaderBlockFragment(int maxHeaderBlockFragment) {
        this.maxHeaderBlockFragment = maxHeaderBlockFragment;
    }

    public int getInitialStreamRecvWindow() {
        return initialStreamRecvWindow;
    }

    public void setInitialStreamRecvWindow(int initialStreamRecvWindow) {
        this.initialStreamRecvWindow = initialStreamRecvWindow;
    }

    public int getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }

    public void setMaxConcurrentStreams(int maxConcurrentStreams) {
        this.maxConcurrentStreams = maxConcurrentStreams;
    }

    public int getInitialSessionRecvWindow() {
        return initialSessionRecvWindow;
    }

    public void setInitialSessionRecvWindow(int initialSessionRecvWindow) {
        this.initialSessionRecvWindow = initialSessionRecvWindow;
    }

    public long getStreamIdleTimeout() {
        return streamIdleTimeout;
    }

    public void setStreamIdleTimeout(long streamIdleTimeout) {
        this.streamIdleTimeout = streamIdleTimeout;
    }

    public long getMaxUploadFileSize() {
        return maxUploadFileSize;
    }

    public void setMaxUploadFileSize(long maxUploadFileSize) {
        this.maxUploadFileSize = maxUploadFileSize;
    }

    public long getMaxRequestBodySize() {
        return maxRequestBodySize;
    }

    public void setMaxRequestBodySize(long maxRequestBodySize) {
        this.maxRequestBodySize = maxRequestBodySize;
    }

    public int getUploadFileSizeThreshold() {
        return uploadFileSizeThreshold;
    }

    public void setUploadFileSizeThreshold(int uploadFileSizeThreshold) {
        this.uploadFileSizeThreshold = uploadFileSizeThreshold;
    }

    @Override
    public String toString() {
        return "HttpConfig{" +
                "timeout=" + timeout +
                ", connectionPoolSize=" + connectionPoolSize +
                ", leakDetectorInterval=" + leakDetectorInterval +
                ", releaseTimeout=" + releaseTimeout +
                ", headerBufferSize=" + headerBufferSize +
                ", contentBufferSize=" + contentBufferSize +
                ", maxDynamicTableSize=" + maxDynamicTableSize +
                ", maxHeaderSize=" + maxHeaderSize +
                ", maxHeaderBlockFragment=" + maxHeaderBlockFragment +
                ", initialStreamRecvWindow=" + initialStreamRecvWindow +
                ", maxConcurrentStreams=" + maxConcurrentStreams +
                ", initialSessionRecvWindow=" + initialSessionRecvWindow +
                ", streamIdleTimeout=" + streamIdleTimeout +
                ", maxUploadFileSize=" + maxUploadFileSize +
                ", maxRequestBodySize=" + maxRequestBodySize +
                ", uploadFileSizeThreshold=" + uploadFileSizeThreshold +
                '}';
    }
}
