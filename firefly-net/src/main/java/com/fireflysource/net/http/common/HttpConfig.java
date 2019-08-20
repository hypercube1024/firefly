package com.fireflysource.net.http.common;

import com.fireflysource.common.coroutine.CoroutineDispatchers;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;

public class HttpConfig {

    public static int DEFAULT_WINDOW_SIZE = 65535;

    private long timeout = 30;
    private int connectionPoolSize = CoroutineDispatchers.INSTANCE.getDefaultPoolSize();
    private long leakDetectorInterval = 60;
    private long releaseTimeout = 60;
    private int requestHeaderBufferSize = 8 * 1024;
    private int contentBufferSize = 8 * 1024;
    private SecureEngineFactory secureEngineFactory;
    private int maxDynamicTableSize = 8 * 1024;
    private int maxHeaderSize = 8 * 1024;
    private int maxHeaderBlockFragment = 0;
    private int initialStreamRecvWindow = 8 * 1024 * 1024;
    private int maxConcurrentPushedStreams = 32;
    private int initialSessionRecvWindow = 16 * 1024 * 1024;
    private int maxLocalStreams = -1;
    private int maxRemoteStreams = -1;

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

    public int getRequestHeaderBufferSize() {
        return requestHeaderBufferSize;
    }

    public void setRequestHeaderBufferSize(int requestHeaderBufferSize) {
        this.requestHeaderBufferSize = requestHeaderBufferSize;
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

    public int getMaxConcurrentPushedStreams() {
        return maxConcurrentPushedStreams;
    }

    public void setMaxConcurrentPushedStreams(int maxConcurrentPushedStreams) {
        this.maxConcurrentPushedStreams = maxConcurrentPushedStreams;
    }

    public int getInitialSessionRecvWindow() {
        return initialSessionRecvWindow;
    }

    public void setInitialSessionRecvWindow(int initialSessionRecvWindow) {
        this.initialSessionRecvWindow = initialSessionRecvWindow;
    }

    public int getMaxLocalStreams() {
        return maxLocalStreams;
    }

    public void setMaxLocalStreams(int maxLocalStreams) {
        this.maxLocalStreams = maxLocalStreams;
    }

    public int getMaxRemoteStreams() {
        return maxRemoteStreams;
    }

    public void setMaxRemoteStreams(int maxRemoteStreams) {
        this.maxRemoteStreams = maxRemoteStreams;
    }

    @Override
    public String toString() {
        return "HttpClientConfig{" +
                "timeout=" + timeout +
                ", connectionPoolSize=" + connectionPoolSize +
                ", leakDetectorInterval=" + leakDetectorInterval +
                ", releaseTimeout=" + releaseTimeout +
                ", requestHeaderBufferSize=" + requestHeaderBufferSize +
                ", contentBufferSize=" + contentBufferSize +
                ", secureEngineFactory=" + secureEngineFactory +
                ", maxDynamicTableSize=" + maxDynamicTableSize +
                ", maxHeaderSize=" + maxHeaderSize +
                ", maxHeaderBlockFragment=" + maxHeaderBlockFragment +
                ", initialStreamRecvWindow=" + initialStreamRecvWindow +
                ", maxConcurrentPushedStreams=" + maxConcurrentPushedStreams +
                ", initialSessionRecvWindow=" + initialSessionRecvWindow +
                '}';
    }
}
