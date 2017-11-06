package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;

/**
 * @author Pengtao Qiu
 */
public class SimpleHTTPClientConfiguration extends HTTP2Configuration {

    public static final int defaultPoolSize = Integer.getInteger("com.firefly.client.http2.connection.defaultPoolSize", 16);
    public static final long defaultConnectTimeout = Long.getLong("com.firefly.client.http2.connection.defaultConnectTimeout", 30 * 1000L);

    private int poolSize = defaultPoolSize;
    private long connectTimeout = defaultConnectTimeout;

    /**
     * Get the HTTP client connection pool size.
     *
     * @return The HTTP client connection pool size.
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Set the HTTP client connection pool size.
     *
     * @param poolSize The HTTP client connection pool size
     */
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    /**
     * Get the connecting timeout. The time unit is millisecond.
     *
     * @return The connecting timeout. The time unit is millisecond.
     */
    public long getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Set the connecting timeout. The time unit is millisecond.
     *
     * @param connectTimeout The connecting timeout. The time unit is millisecond.
     */
    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}
