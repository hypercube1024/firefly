package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.utils.ServiceUtils;
import com.firefly.utils.heartbeat.HealthCheck;

/**
 * @author Pengtao Qiu
 */
public class SimpleHTTPClientConfiguration extends HTTP2Configuration {

    public static final int defaultPoolSize = Integer.getInteger("com.firefly.client.http2.connection.defaultPoolSize", 16);
    public static final long defaultConnectTimeout = Long.getLong("com.firefly.client.http2.connection.defaultConnectTimeout", 10 * 1000L);

    private int poolSize = defaultPoolSize;
    private long connectTimeout = defaultConnectTimeout;
    private long leakDetectorInterval = 60 * 10; // unit second
    private int maxGettingThreadNum = 4;
    private int maxReleaseThreadNum = 4;
    private HealthCheck healthCheck = ServiceUtils.loadService(HealthCheck.class, new HealthCheck());

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

    /**
     * Get the HealthCheck. It checks the HTTP client connection is alive.
     *
     * @return the HealthCheck.
     */
    public HealthCheck getHealthCheck() {
        return healthCheck;
    }

    /**
     * Set the HealthCheck. It checks the HTTP client connection is alive.
     *
     * @param healthCheck the HealthCheck.
     */
    public void setHealthCheck(HealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }

    public long getLeakDetectorInterval() {
        return leakDetectorInterval;
    }

    public void setLeakDetectorInterval(long leakDetectorInterval) {
        this.leakDetectorInterval = leakDetectorInterval;
    }

    public int getMaxGettingThreadNum() {
        return maxGettingThreadNum;
    }

    public void setMaxGettingThreadNum(int maxGettingThreadNum) {
        this.maxGettingThreadNum = maxGettingThreadNum;
    }

    public int getMaxReleaseThreadNum() {
        return maxReleaseThreadNum;
    }

    public void setMaxReleaseThreadNum(int maxReleaseThreadNum) {
        this.maxReleaseThreadNum = maxReleaseThreadNum;
    }
}
