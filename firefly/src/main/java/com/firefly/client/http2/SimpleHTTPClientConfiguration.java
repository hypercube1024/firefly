package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;

/**
 * @author Pengtao Qiu
 */
public class SimpleHTTPClientConfiguration extends HTTP2Configuration {

    private int poolSize = 16;
    private long connectTimeout = 5000L;

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}
