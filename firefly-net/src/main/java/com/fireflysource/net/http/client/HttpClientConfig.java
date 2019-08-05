package com.fireflysource.net.http.client;

import com.fireflysource.common.coroutine.CoroutineDispatchers;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;

public class HttpClientConfig {

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

    @Override
    public String toString() {
        return "HttpClientConfig{" +
                "timeout=" + timeout +
                ", connectionPoolSize=" + connectionPoolSize +
                ", leakDetectorInterval=" + leakDetectorInterval +
                ", releaseTimeout=" + releaseTimeout +
                ", requestHeaderBufferSize=" + requestHeaderBufferSize +
                ", contentBufferSize=" + contentBufferSize +
                ", maxDynamicTableSize=" + maxDynamicTableSize +
                ", maxHeaderSize=" + maxHeaderSize +
                '}';
    }
}
