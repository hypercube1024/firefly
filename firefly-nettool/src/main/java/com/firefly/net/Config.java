package com.firefly.net;

import com.firefly.utils.ServiceUtils;

public class Config {

    private int timeout = 30 * 1000;

    // asynchronous I/O thread pool settings
    private int asynchronousCorePoolSize = Runtime.getRuntime().availableProcessors();
    private int asynchronousPoolKeepAliveTime = 15 * 1000;

    private String serverName = "firefly-server";
    private String clientName = "firefly-client";

    private Decoder decoder;
    private Encoder encoder;
    private Handler handler;

    private boolean monitorEnable = true;
    private MetricReporterFactory metricReporterFactory = ServiceUtils.loadService(MetricReporterFactory.class, new DefaultMetricReporterFactory());

    /**
     * The max I/O idle time, the default value is 10 seconds.
     *
     * @return The max I/O idle time，the unit is MS.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * The I/O timeout, if the last I/O timestamp before present over timeout
     * value, the session will close.
     *
     * @param timeout the max I/O idle time，the unit is MS.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public int getAsynchronousCorePoolSize() {
        return asynchronousCorePoolSize;
    }

    public void setAsynchronousCorePoolSize(int asynchronousCorePoolSize) {
        this.asynchronousCorePoolSize = asynchronousCorePoolSize;
    }

    public int getAsynchronousPoolKeepAliveTime() {
        return asynchronousPoolKeepAliveTime;
    }

    public void setAsynchronousPoolKeepAliveTime(int asynchronousPoolKeepAliveTime) {
        this.asynchronousPoolKeepAliveTime = asynchronousPoolKeepAliveTime;
    }

    public MetricReporterFactory getMetricReporterFactory() {
        return metricReporterFactory;
    }

    public void setMetricReporterFactory(MetricReporterFactory metricReporterFactory) {
        this.metricReporterFactory = metricReporterFactory;
    }

    public boolean isMonitorEnable() {
        return monitorEnable;
    }

    public void setMonitorEnable(boolean monitorEnable) {
        this.monitorEnable = monitorEnable;
    }

    @Override
    public String toString() {
        return "Config{" +
                "timeout=" + timeout +
                ", asynchronousCorePoolSize=" + asynchronousCorePoolSize +
                ", asynchronousPoolKeepAliveTime=" + asynchronousPoolKeepAliveTime +
                '}';
    }
}
