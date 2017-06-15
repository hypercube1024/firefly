package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;

/**
 * @author Pengtao Qiu
 */
public class SimpleHTTPClientConfiguration extends HTTP2Configuration {

    public static final int defaultPoolSize = Integer.getInteger("com.firefly.client.http2.connection.defaultPoolSize", 64);
    public static final long defaultConnectTimeout = Long.getLong("com.firefly.client.http2.connection.defaultConnectTimeout", 30 * 1000L);

    private int poolSize = defaultPoolSize;
    private long connectTimeout = defaultConnectTimeout;

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
